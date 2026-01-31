# Firestore rules (shared lists)

**Paste only the block below** in Firebase Console → Firestore Database → **Rules** tab, then click **Publish**.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // profile docs
    match /users/{userId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }

    // Shared lists: any signed-in user can read (so "Join by invite code" can find the list).
    // Only owner/members can update/delete; a user can update to add or remove themselves.
    match /sharedLists/{listId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null
        && (resource.data.ownerId == request.auth.uid
            || request.auth.uid in resource.data.memberIds
            || (request.auth.uid in request.resource.data.memberIds && !(request.auth.uid in resource.data.memberIds))
            || (request.auth.uid in resource.data.memberIds && !(request.auth.uid in request.resource.data.memberIds)));
      allow delete: if request.auth != null
        && (resource.data.ownerId == request.auth.uid
            || request.auth.uid in resource.data.memberIds);
    }
    match /sharedLists/{listId}/checklistItems/{itemId} {
      allow read, write: if request.auth != null
        && (get(/databases/$(database)/documents/sharedLists/$(listId)).data.ownerId == request.auth.uid
            || request.auth.uid in get(/databases/$(database)/documents/sharedLists/$(listId)).data.memberIds);
    }
  }
}
```
