const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-framemem-9c0f1cf0-c9b6-4ee2-9a19-1271d373899f";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function dbFor(uid) {
  return uid
    ? testEnv.authenticatedContext(uid).firestore(DATABASE_ID)
    : testEnv.unauthenticatedContext().firestore(DATABASE_ID);
}

function validSolvedPayload(uid, now = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    queryText: "Guy in a yellow raincoat, a lighthouse, someone says the tide is wrong",
    filterSummary: "2010s • USA • Live-Action",
    title: "The Lighthouse",
    year: 2019,
    mediaType: "MOVIE",
    country: "USA",
    style: "LIVE_ACTION",
    languageMode: "ORIGINAL",
    directorOrCreator: "Robert Eggers",
    whyItMatched: "Set at a remote New England lighthouse with oilskin raincoats and shifting tides.",
    iconicCue: "Why'd ya spill yer beans?",
    posterUrl: "https://example.com/poster.jpg",
    personalNote: "Rewatch this weekend",
    sharedToCommunity: false,
    communityMemoryId: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validCommunityPayload(uid, now = new Date(Date.now() - 1000)) {
  return {
    authorId: uid,
    authorName: "Alice",
    status: "PUBLISHED",
    queryText: "Animated train floating over flooded ocean tracks with a silent spirit",
    filterSummary: "2000s • Japan • Animated",
    title: "Spirited Away",
    year: 2001,
    mediaType: "ANIME",
    country: "Japan",
    style: "ANIMATED",
    languageMode: "SUBTITLED",
    directorOrCreator: "Hayao Miyazaki",
    whyItMatched: "Chihiro boards the Sea Railway train across shallow endless water with No-Face.",
    iconicCue: "Sixth Station train ride over the water",
    posterUrl: "https://example.com/spirited.jpg",
    yesCount: 0,
    noCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write personal vault or community memories", async () => {
  const unauthDb = dbFor(null);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("solved_memories").get());
  await assertFails(unauthDb.collection("community_memories").where("status", "==", "PUBLISHED").get());
  await assertFails(
    unauthDb.collection("community_memories").doc("gem_1").set(validCommunityPayload(ALICE_UID))
  );
});

test("Solved memories: owner create, read, partial update, delete succeed; cross-user & ghost fields fail", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").set(
      validSolvedPayload(ALICE_UID, created)
    )
  );

  // List & get own memories
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").get()
  );

  // Partial update of note & shared status
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").update({
      personalNote: "Found it! Classic A24.",
      sharedToCommunity: true,
      communityMemoryId: "comm_1",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject modifying immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject including @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_2").set({
      id: "mem_2",
      ...validSolvedPayload(ALICE_UID, created),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_3").set(
      validSolvedPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Cross-user isolation: Bob cannot read, update, or delete Alice's memory
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").update({
      personalNote: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").delete()
  );

  // Owner can delete
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("solved_memories").doc("mem_1").delete()
  );
});

test("Community memories: filtered query succeeds, unfiltered query fails, atomic batch vote succeeds", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const created = new Date(Date.now() - 5000);

  // Alice publishes a community gem
  await assertSucceeds(
    aliceDb.collection("community_memories").doc("comm_1").set(
      validCommunityPayload(ALICE_UID, created)
    )
  );

  // Filtered query matching rule succeeds
  await assertSucceeds(
    bobDb.collection("community_memories").where("status", "==", "PUBLISHED").get()
  );

  // Unfiltered query on top-level collection fails
  await assertFails(bobDb.collection("community_memories").get());

  // Bob cannot inflate yesCount without writing his atomic vote record
  await assertFails(
    bobDb.collection("community_memories").doc("comm_1").update({
      yesCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob votes YES atomically via WriteBatch (votes/{uid} + my_votes/{id} + community_memories/{id})
  const now = new Date(Date.now() - 500);
  const batch = bobDb.batch();
  batch.set(
    bobDb.collection("community_memories").doc("comm_1").collection("votes").doc(BOB_UID),
    {
      userId: BOB_UID,
      memoryId: "comm_1",
      vote: "YES",
      createdAt: now,
      updatedAt: now,
    }
  );
  batch.set(
    bobDb.collection("users").doc(BOB_UID).collection("my_votes").doc("comm_1"),
    {
      userId: BOB_UID,
      memoryId: "comm_1",
      vote: "YES",
      createdAt: now,
      updatedAt: now,
    }
  );
  batch.update(bobDb.collection("community_memories").doc("comm_1"), {
    yesCount: 1,
    noCount: 0,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Bob cannot jump yesCount by +5 even with a vote doc
  const badBatch = bobDb.batch();
  badBatch.update(
    bobDb.collection("community_memories").doc("comm_1").collection("votes").doc(BOB_UID),
    { vote: "NO", updatedAt: new Date(Date.now() - 200) }
  );
  badBatch.update(bobDb.collection("community_memories").doc("comm_1"), {
    yesCount: 6,
    noCount: 0,
    updatedAt: new Date(Date.now() - 200),
  });
  await assertFails(badBatch.commit());

  // Bob cannot delete Alice's community gem
  await assertFails(bobDb.collection("community_memories").doc("comm_1").delete());

  // Alice can delete her own community gem
  await assertSucceeds(aliceDb.collection("community_memories").doc("comm_1").delete());
});
