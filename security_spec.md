# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Zero Unauthenticated Access**: Every read and write across `/admins`, `/users`, `/scores`, `/game_sessions`, `/referrals`, `/friends`, `/teams`, `/competitions`, `/challenges`, `/notifications`, `/game_settings`, and `/items` requires `request.auth != null`.
2. **User Isolation & PII Protection**: A user document `/users/{userId}` can only be read (`get`/`list`) or mutated by its owner (`request.auth.uid == userId`) or a verified administrator (`isAdmin()`). Users cannot self-assign `suspended = true/false` on update or spoof another user's `userId`.
3. **Anti-Cheat Score & Session Bounds**: `/scores/{scoreId}` and `/game_sessions/{sessionId}` enforce strict numeric bounds (`coinsEarned` 0..50000, `itemsCollected` 0..500, `bestCombo` 1..100, `durationSeconds` 10..600). List queries on `/scores` MUST filter by `status == 'valid'` or `userId == request.auth.uid` (unless caller is `isAdmin()`).
4. **Referral Anti-Abuse**: `/referrals/{referralId}` enforces `referredUserId == request.auth.uid` and `referrerUserId != request.auth.uid` (preventing self-referral) and caps `rewardCoins` at `0..1000`.
5. **Multiplayer Rooms, Teams, Friends & Challenges**: `/teams/{teamId}`, `/competitions/{competitionId}`, `/challenges/{challengeId}`, `/friends/{friendshipId}`, and `/notifications/{notificationId}` enforce strict schema keys, string/list size limits, and creator/manager ownership invariants.
6. **Admin-Only Configuration**: `/game_settings/{configId}` and `/items/{itemId}` can only be created, updated, or deleted by `isAdmin()`.
7. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be server timestamps (`is timestamp && <= request.time`), and `createdAt` is immutable on update.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: Unauthenticated client attempts `get` or `list` on `/users`, `/scores`, `/teams`, or `/competitions`.
2. **Cross-User Profile Read**: Authenticated `alice_123` attempts to `get` `/users/bob_456`.
3. **Identity Spoofing on Create**: `alice_123` attempts to create `/scores/score_1` or `/game_sessions/sess_1` with `userId: "bob_456"`.
4. **Self-Referral Abuse**: `alice_123` attempts to create `/referrals/ref_self` where `referrerUserId == referredUserId`.
5. **Self-Unsuspend Privilege Escalation**: Suspended user `alice_123` attempts to update `/users/alice_123` with `"suspended": false`.
6. **Impossible Anti-Cheat Score Injection**: `alice_123` attempts to create `/scores/score_cheat` with `"coinsEarned": 9999999`.
7. **Unfiltered Score Scraping**: Non-admin `alice_123` runs unfiltered `db.collection("scores").get()` where a flagged score belonging to `bob_456` exists.
8. **Score Mutation by Player**: `alice_123` attempts to update her existing `/scores/score_1` to increase `coinsEarned`.
9. **Unauthorized Game Settings Tampering**: Non-admin `alice_123` attempts to update `/game_settings/global`.
10. **Unauthorized Item Value Tampering**: Non-admin `alice_123` attempts to update `/items/mango` with `"coinValue": 1000`.
11. **Email Spoofing Attack**: User with `email: "MDJUBAYERHOSEN302@gmail.com"` but `email_verified: false` attempts admin write on `/game_settings/global`.
12. **Resource / Value Poisoning**: User attempts to write a 10KB string into `username` or an array of 50 elements into `unlockedAchievements`.
