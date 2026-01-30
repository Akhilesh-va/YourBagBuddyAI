# Backend Migration Checklist — YourBagBuddy

This document lists **critical** steps to migrate from client-only (Firebase + AI from device) to a **backend-powered, production-ready** setup. It covers AI, user database, auth, and security.

---

## 1. Move API Keys & Secrets Off the Device

### Current risk
- **OPENROUTER_API_KEY** and **ZAI_API_KEY** are in `local.properties` and exposed via `BuildConfig` → anyone who decompiles the APK can extract and abuse them.
- `OpenRouterTravelAi` and `SmartPackRepositoryImpl` → ZaiChatService call AI providers directly from the app.

### What to do

| Task | Priority | Notes |
|------|----------|--------|
| **Stop sending API keys from the app** | **Critical** | Remove `OPENROUTER_API_KEY`, `ZAI_API_KEY` from `BuildConfig` and from any code that calls OpenRouter/Z.AI from the client. |
| **Proxy all AI through your backend** | **Critical** | App calls *your* API (e.g. `POST /api/ai/packing-list`, `GET /api/ai/travel-tip`). Backend holds keys in env vars and calls OpenRouter/Z.AI server-side. |
| **Store keys only in backend env** | **Critical** | Use env vars (e.g. `.env` / secrets manager) on the server. Never commit keys to git. |
| **Optional: per-user or per-app quotas** | High | Backend can enforce rate limits and quota per user/app before calling AI. |

**App-side change:** Replace `ZaiChatService` (and `OpenRouterTravelAi`) with a new API client that talks to **your** backend endpoints. Backend then calls OpenRouter/Z.AI with the server-held key.

---

## 2. Authentication & Authorisation

### Current state
- Firebase Auth (email/password, Google, phone, email link).
- User profiles in Firestore `users/{userId}`.
- Trips/checklists under `users/{userId}/trips` etc., with `userId` coming from `authRepository.getCurrentUser()?.id`.

### What to do

| Task | Priority | Notes |
|------|----------|--------|
| **Validate Firebase ID tokens on the backend** | **Critical** | For every backend API call, client sends Firebase ID token (or custom token). Backend verifies it with Firebase Admin SDK and uses `uid` as the authenticated user. |
| **Issue short-lived session/JWT from backend** | High | Option A: backend validates Firebase token once, issues its own JWT (e.g. 15–60 min). Option B: send Firebase token on each request. A reduces load on Firebase; B is simpler. |
| **Tie every API to a user** | **Critical** | All trips, checklists, AI requests must be bound to the verified `uid`. Never trust `userId` from request body; always use the one from the verified token. |
| **Keep Firebase Auth on the client** | Normal | Continue using Firebase Auth in the app; only the *backend* must verify tokens and enforce “who is this user?” |

**Backend pattern:**  
`Authorization: Bearer <firebase-id-token-or-your-jwt>` → verify token → `uid` → use `uid` in DB queries and rate limits.

---

## 3. User Database & Data Access

### Current state
- Firestore: `users`, `users/{userId}/trips`, and checklist-related data.
- Room locally; Firestore for sync.
- No `firestore.rules` in repo (relies on project default or Console rules).

### What to do

| Task | Priority | Notes |
|------|----------|--------|
| **Enforce strict Firestore rules** | **Critical** | If you keep Firestore, rules must enforce: `request.auth != null` and `request.auth.uid == userId` for `users/{userId}/...`. Without this, a modified client could read/write other users’ data. |
| **Or move persistence to your backend** | High | Alternative: backend owns a DB (e.g. Postgres). App talks only to your API; backend reads/writes DB using verified `uid`. Firestore then only for optional real-time features or legacy migration. |
| **Encrypt sensitive fields at rest** | High | Especially if you store health/medicine or PII. Use application-level encryption or DB-level encryption (e.g. TDE). |
| **Backup user and trip data** | **Critical** | Automated backups (daily at least), retention policy, and tested restore. |
| **Audit logging** | High | Log who accessed/created/updated what and when (user id, resource id, action, timestamp). Store in a separate audit store. |

**Security rule example (if you keep Firestore):**

```javascript
// Only the authenticated user can read/write their own document and subcollections
match /users/{userId}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

---

## 4. AI Request Security & Production Hardening

### What to do

| Task | Priority | Notes |
|------|----------|--------|
| **Route all AI through backend** | **Critical** | Packing list, travel tips, any future AI — client → your API → AI provider. Keys stay on server. |
| **Validate and sanitise input** | **Critical** | Validate `SmartPackRequest` (destination, dates, length, etc.). Reject oversized or malformed input. Sanitise before building prompts to avoid prompt injection. |
| **Rate limit per user** | **Critical** | Limit calls to AI endpoints per user (and per IP if needed) to avoid abuse and control cost. |
| **Scope AI to the user** | **Critical** | Backend only uses data for the authenticated `uid` when building context (e.g. “user’s past trips”, “user’s medicines”). |
| **Structured error handling** | High | Return stable error codes and safe messages to the app; log full details and PII only server-side. |
| **Timeouts and circuit breakers** | High | Timeout calls to OpenRouter/Z.AI; optionally use a circuit breaker to fail fast when the provider is down. |
| **Cost and usage monitoring** | High | Log token usage and cost per user/request to detect spikes and enforce budgets. |

**Backend endpoints (examples):**

- `POST /api/ai/packing-list` — body: SmartPackRequest-like JSON; auth required; rate limited.
- `GET /api/ai/travel-tip` — auth required; rate limited.
- `POST /api/ai/chat` — body: `{ "message": string, "history": [{ "role": "user"|"assistant", "content": string }] }`; returns `{ "reply": string, "error": string? }`. Same AI as packing list; auth required; rate limited.
- Response: same shape your app already expects (e.g. list of checklist items, a tip string, or chat reply).

---

## 5. Network & Transport Security

| Task | Priority | Notes |
|------|----------|--------|
| **HTTPS only** | **Critical** | All app ↔ backend and backend ↔ AI/DB must use TLS. |
| **Certificate pinning (optional)** | Medium | For high-security needs, pin your backend’s certificate in the app. |
| **Secure backend config** | **Critical** | No secrets in code; use env vars or a secrets manager. Rotate keys periodically. |

---

## 6. Production Readiness

| Task | Priority | Notes |
|------|----------|--------|
| **Environment config** | **Critical** | Separate dev/staging/prod: different API base URLs, DBs, and keys. App gets backend URL from build flavour or config. |
| **Health checks** | High | e.g. `GET /health` for load balancer and monitoring. |
| **Structured logging** | High | JSON logs, trace ids, no sensitive data in logs. |
| **Error tracking** | High | e.g. Sentry/Cloud Watch to capture backend exceptions. |
| **Monitoring & alerts** | High | Latency, error rate, AI usage; alert on anomalies. |
| **Database migrations** | High | If you use a SQL DB, use versioned migrations (e.g. Flyway, Liquibase). |
| **GDPR / privacy** | High | Data retention, right to delete, privacy policy; document where AI providers send data. |

---

## 7. App-Side Changes Summary

1. **Remove AI keys from the app**  
   - Stop using `BuildConfig.OPENROUTER_API_KEY` / `ZAI_API_KEY` for external AI calls.

2. **Point AI to your backend**  
   - New `YourBackendApi` (or similar) with endpoints like:
     - `POST /api/ai/packing-list` (replacing direct OpenRouter call in `SmartPackRepositoryImpl`).
     - `GET /api/ai/travel-tip` (replacing `OpenRouterTravelAi.fetchTravelTip()`).
   - `SmartPackRepositoryImpl` and the code that uses `OpenRouterTravelAi` call these endpoints instead.

3. **Send auth with every request**  
   - Attach Firebase ID token (or backend-issued JWT) in `Authorization` for all backend calls.

4. **Backend base URL**  
   - From BuildConfig or build flavour (e.g. `BuildConfig.API_BASE_URL`) so you can switch dev/staging/prod.

5. **Keep Firebase for auth and, if you keep it, for data**  
   - Auth stays in the app via Firebase; backend verifies tokens and owns or tightly constrains access to user data.

---

## 8. Suggested Order of Work

1. **Secrets & AI proxy** — Implement backend endpoints that call OpenRouter/Z.AI; remove keys from the app; app calls backend only.
2. **Auth on backend** — Verify Firebase (or your) tokens; attach `uid` to every request.
3. **User database** — Harden Firestore rules **or** move persistence to your DB and expose via API.
4. **Rate limiting & validation** — For AI and other sensitive endpoints.
5. **Logging, monitoring, backups** — So you can run and recover production safely.

Using this checklist will get you to a **secure, production-level** setup where API keys are off the device, every request is tied to a real user, and AI and user data are under your control.
