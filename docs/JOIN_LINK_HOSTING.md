# Step-by-step: Make the join link clickable (HTTPS)

The app shares an HTTPS link (e.g. `https://yourbagbuddy-ai.firebaseapp.com/join/AMMUXY`) so it’s **clickable** in WhatsApp, SMS, etc. Follow these steps to deploy the redirect page so the link works when tapped.

---

## Prerequisites

- Node.js installed on your computer ([nodejs.org](https://nodejs.org))
- Your Android project already uses Firebase (same project as your app)

---

## Step 1: Open a terminal

Open Terminal (Mac/Linux) or Command Prompt / PowerShell (Windows) and go to your project root (the folder that contains `firebase.json` and the `app` folder):

```bash
cd /path/to/YourBagBuddy
```

Replace `/path/to/YourBagBuddy` with the actual path to your project (e.g. `cd ~/Developer/YouBagBuddy\ AI/YourBagBuddy`).

---

## Step 2: Install Firebase CLI (one-time)

If you have never used Firebase CLI on this machine:

```bash
npm install -g firebase-tools
```

If you get a permission error, use:

```bash
sudo npm install -g firebase-tools
```

---

## Step 3: Log in to Firebase (one-time)

```bash
firebase login
```

A browser window will open. Sign in with the same Google account you use for your Firebase project (yourbagbuddy-ai).

---

## Step 4: Link this folder to your Firebase project (one-time)

If this project folder is not yet linked to Firebase:

```bash
firebase use --add
```

- Select your Firebase project (e.g. **yourbagbuddy-ai**) from the list.
- When asked for an alias, press Enter to use **default**.

If you already ran `firebase init` or `firebase use` here before, you can skip this step.

---

## Step 5: Deploy Hosting

From the project root (where `firebase.json` and the `hosting` folder are):

```bash
firebase deploy --only hosting
```

Wait until you see **Deploy complete** and a **Hosting URL** (e.g. `https://yourbagbuddy-ai.web.app` or `https://yourbagbuddy-ai.firebaseapp.com`).

---

## Step 6: Confirm the join link works

1. In a browser, open:  
   `https://yourbagbuddy-ai.firebaseapp.com/join/AMMUXY`  
   (or use your Hosting URL if different; replace **AMMUXY** with any 6-letter invite code).

2. You should see the YourBagBuddy redirect page (“Opening the app to join the list…”).  
   - On a phone with the app installed, it may open the app or show “Open with YourBagBuddy”.  
   - On a computer, you’ll see the page with “Open in app” and “Get the app on Play Store”.

3. In the app, use **Share with group** and send the message to yourself (e.g. WhatsApp). The link in the message should be **blue/clickable**. Tapping it should open the redirect page or the app.

---

## Step 7 (optional): Update Play Store link after publishing

When your app is on the Play Store, edit **`hosting/join.html`** and replace the Play Store URL:

- Find: `com.example.yourbagbuddy`  
- Replace with your real package name (e.g. `com.yourcompany.yourbagbuddy`).

Then deploy again:

```bash
firebase deploy --only hosting
```

---

## Quick reference

| Step              | Command / action                          |
|------------------|--------------------------------------------|
| Go to project    | `cd /path/to/YourBagBuddy`                 |
| Install CLI      | `npm install -g firebase-tools`            |
| Log in           | `firebase login`                           |
| Link project     | `firebase use --add` (only if needed)      |
| Deploy hosting   | `firebase deploy --only hosting`           |
| Test join link   | Open `https://yourbagbuddy-ai.firebaseapp.com/join/AMMUXY` in a browser |

---

## Troubleshooting

- **“No project active”**  
  Run `firebase use --add` and select your Firebase project.

- **“Permission denied” when installing CLI**  
  Use `sudo npm install -g firebase-tools` (Mac/Linux) or run the terminal as Administrator (Windows).

- **Link still not clickable in WhatsApp**  
  Make sure you’re sharing the **HTTPS** link (the app now uses it by default). If you have an old message with `yourbagbuddy://...`, send a new message after “Share with group” so it contains the new link.

- **Different Firebase Hosting URL**  
  If your Hosting URL is not `yourbagbuddy-ai.firebaseapp.com`, the app is still using that host in the share link. Either use the same Firebase project for Hosting, or you’ll need to change the link base in the app (e.g. in `MainActivity.JOIN_LINK_HTTPS_HOST`) to match your Hosting URL.
