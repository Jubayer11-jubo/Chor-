package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChorRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_submitRoundResult_recordsScoreAndGameSession() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ChorRepository(firestore, auth)
        val dataManager = repository.dataManager

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("AliceChor")
        }
        assertTrue("Expected profile creation to succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertEquals(uid, profile.userId)
        assertTrue(profile.referralCode.startsWith("JR"))

        val scoreResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitRoundResult(
                profile = profile,
                coinsEarned = 320L,
                itemsCollected = 15L,
                bestCombo = 4L,
                durationSeconds = 150L,
                mostCollectedItem = "আম",
                mangoesInRound = 6L,
                coconutsInRound = 4L,
                cowsInRound = 1L,
                reachedSpeedThief = false,
                gameMode = "CHOR"
            )
        }
        assertTrue("Expected score submission to succeed: ${scoreResult.exceptionOrNull()}", scoreResult.isSuccess)

        val emittedScores = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeValidScores().first { list -> list.any { it.userId == uid } }
        }
        assertTrue(emittedScores.any { it.coinsEarned == 320L })

        val emittedSessions = withTimeout(FLOW_TIMEOUT_MS) {
            dataManager.observeUserGameSessions(uid).first { list -> list.any { it.userId == uid } }
        }
        assertTrue(emittedSessions.any { it.coinsEarned == 320L && it.gameMode == "CHOR" })
    }

    @Test
    fun dataManager_createTeam_competition_and_referral_succeeds() = runBlocking {
        val uniqueEmail = "jubayer_${System.currentTimeMillis()}@test.com"
        signInTestUser(uniqueEmail)
        val repository = ChorRepository(firestore, auth)
        val dataManager = repository.dataManager

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.saveOrFetchUser("Jubayer").getOrThrow()
        }

        // Self-referral should be rejected
        val selfRef = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.applyReferralCode(profile, profile.referralCode, 100L)
        }
        assertTrue("Expected self-referral to fail", selfRef.isFailure)

        // Valid referral code from another user succeeds
        val validRef = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.applyReferralCode(profile, "JRBOB99", 100L)
        }
        assertTrue("Expected valid referral to succeed: ${validRef.exceptionOrNull()}", validRef.isSuccess)

        // Create team
        val teamRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.createTeam(profile, "চোর বাহিনী", "🥷")
        }
        assertTrue("Expected team creation to succeed: ${teamRes.exceptionOrNull()}", teamRes.isSuccess)

        // Create private competition room
        val compRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.createCompetitionRoom(profile, "Jubayer vs Friends", "CHOR", 150L)
        }
        assertTrue("Expected competition room creation to succeed: ${compRes.exceptionOrNull()}", compRes.isSuccess)
    }

    @Test
    fun getUserProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ChorRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("AliceChor").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ChorRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfileById(aliceUid)
        }
        assertTrue("Expected cross-user profile read to fail", crossReadResult.isFailure)
        val ex = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeValidScores_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ChorRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeValidScores().first()
            }
            fail("Expected FirebaseFirestoreException for unauthenticated user")
        } catch (e: Exception) {
            val firestoreEx = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected FirebaseFirestoreException in cause chain, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_chor@test.com"
        const val BOB_EMAIL = "bob_chor@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
