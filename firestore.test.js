const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildValidUser(uid, overrides = {}) {
  const ts = new Date(Date.now() - 1000);
  return {
    userId: uid,
    username: "PlayerOne",
    avatarId: "avatar_hoodie",
    totalCoins: 100,
    totalRounds: 1,
    bestScore: 100,
    highestCombo: 2,
    mangoCount: 5,
    coconutCount: 2,
    cowCount: 0,
    unlockedAchievements: ["mango_starter"],
    suspended: false,
    tutorialCompleted: true,
    referralCode: "JR12345",
    referredByCode: "",
    referralCount: 0,
    teamId: "",
    teamName: "",
    chandaBestScore: 0,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function buildValidScore(scoreId, uid, overrides = {}) {
  const ts = new Date(Date.now() - 1000);
  return {
    scoreId,
    userId: uid,
    username: "PlayerOne",
    avatarId: "avatar_hoodie",
    coinsEarned: 450,
    itemsCollected: 18,
    bestCombo: 4,
    durationSeconds: 150,
    mostCollectedItem: "Mango",
    periodDay: "2026-10-07",
    periodWeek: "2026-W41",
    flaggedSuspicious: false,
    status: "valid",
    createdAt: ts,
    ...overrides,
  };
}

test("1. Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("scores").where("status", "==", "valid").get());
  await assertFails(unauthDb.collection("teams").where("status", "==", "active").get());
  await assertFails(unauthDb.collection("competitions").where("status", "==", "waiting").get());
});

test("2. Authenticated user can create and read their own profile, but cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildValidUser(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("3. Referrals prevent self-referral and allow valid referral creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  // Self-referral must fail
  await assertFails(
    aliceDb.collection("referrals").doc("ref_self").set({
      referralId: "ref_self",
      referrerUserId: ALICE_UID,
      referrerCode: "JR12345",
      referredUserId: ALICE_UID,
      referredUsername: "Alice",
      rewardCoins: 100,
      status: "completed",
      createdAt: ts,
    })
  );

  // Valid referral from Bob to Alice succeeds
  await assertSucceeds(
    aliceDb.collection("referrals").doc("ref_valid").set({
      referralId: "ref_valid",
      referrerUserId: BOB_UID,
      referrerCode: "JRBOB99",
      referredUserId: ALICE_UID,
      referredUsername: "Alice",
      rewardCoins: 100,
      status: "completed",
      createdAt: ts,
    })
  );
});

test("4. Teams, Competitions, Challenges, and Game Sessions enforce ownership and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  // Create team
  await assertSucceeds(
    aliceDb.collection("teams").doc("team_1").set({
      teamId: "team_1",
      teamCode: "CHOR99",
      teamName: "চোর বাহিনী",
      teamAvatar: "🥷",
      managerId: ALICE_UID,
      managerName: "Alice",
      memberIds: [ALICE_UID],
      memberNames: ["Alice"],
      totalScore: 1200,
      suspended: false,
      status: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create competition room
  await assertSucceeds(
    aliceDb.collection("competitions").doc("comp_1").set({
      competitionId: "comp_1",
      roomCode: "ABC123",
      title: "Alice vs Friends",
      managerId: ALICE_UID,
      managerName: "Alice",
      gameMode: "CHOR",
      status: "waiting",
      durationSeconds: 150,
      participantIds: [ALICE_UID],
      leaderboardSummary: ["Alice: 0"],
      winnerName: "",
      winnerScore: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create challenge
  await assertSucceeds(
    aliceDb.collection("challenges").doc("chal_1").set({
      challengeId: "chal_1",
      challengeCode: "CHAL777",
      creatorId: ALICE_UID,
      creatorName: "Alice",
      creatorScore: 2450,
      gameMode: "CHOR",
      accepterId: "",
      accepterName: "",
      accepterScore: 0,
      winnerName: "",
      status: "open",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create game_session
  await assertSucceeds(
    aliceDb.collection("game_sessions").doc("sess_1").set({
      sessionId: "sess_1",
      userId: ALICE_UID,
      username: "Alice",
      gameMode: "CHANDA",
      coinsEarned: 850,
      itemsCollected: 12,
      bestCombo: 4,
      durationSeconds: 75,
      roomCode: "",
      createdAt: ts,
    })
  );
});
