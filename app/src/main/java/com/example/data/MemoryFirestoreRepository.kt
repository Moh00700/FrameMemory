package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.CommunityMemory
import com.example.model.CommunityVoteRecord
import com.example.model.SceneCandidate
import com.example.model.SolvedMemory
import com.example.model.VoteChoice
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MemoryFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeSolvedMemories(userId: String): Flow<List<SolvedMemory>> = flow {
        val path = "users/$userId/solved_memories"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("solved_memories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolvedMemory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    if (error !is CancellationException || hasFirestoreCause(error)) {
                        if (auth.currentUser?.uid == userId) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                    }
                    throw error
                }
        )
    }

    fun observeCommunityMemories(): Flow<List<CommunityMemory>> = flow {
        val path = "community_memories"
        emitAll(
            db.collection("community_memories")
                .whereEqualTo("status", "PUBLISHED")
                .limit(60)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityMemory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error !is CancellationException || hasFirestoreCause(error)) {
                        if (auth.currentUser != null) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                    }
                    throw error
                }
        )
    }

    fun observeUserVotes(userId: String): Flow<Map<String, VoteChoice>> = flow {
        val path = "users/$userId/my_votes"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("my_votes")
                .snapshots()
                .map { snapshot ->
                    val records = snapshot.toObjects(
                        CommunityVoteRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    records.associate { it.memoryId to it.voteEnum() }
                }
                .catch { error ->
                    if (error !is CancellationException || hasFirestoreCause(error)) {
                        if (auth.currentUser?.uid == userId) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                    }
                    throw error
                }
        )
    }

    suspend fun saveSolvedMemory(
        userId: String,
        authorName: String,
        queryText: String,
        filterSummary: String,
        candidate: SceneCandidate,
        personalNote: String,
        shareToCommunity: Boolean
    ): Result<String> {
        val solvedRef = db.collection("users").document(userId).collection("solved_memories").document()
        val commRef = db.collection("community_memories").document()
        val docPath = solvedRef.path

        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }

            val cleanQuery = queryText.trim().ifBlank { candidate.title }.take(500)
            val cleanFilter = filterSummary.trim().take(200)
            val cleanTitle = candidate.title.trim().ifBlank { "Untitled" }.take(200)
            val cleanYear = candidate.year.toLong().coerceIn(1880L, 2035L)
            val cleanCountry = candidate.country.trim().ifBlank { "Unknown" }.take(80)
            val cleanCreator = candidate.directorOrCreator.trim().take(120)
            val cleanWhy = candidate.whyItMatched.trim().ifBlank { "Scene match" }.take(1000)
            val cleanCue = candidate.iconicCue.trim().take(400)
            val cleanPoster = candidate.posterUrl.trim().take(800)
            val cleanNote = personalNote.trim().take(500)
            val cleanAuthor = authorName.trim().ifBlank { "Cinephile" }.take(80)

            val batch = db.batch()
            val communityId = if (shareToCommunity) commRef.id else ""

            val solvedPayload = mapOf(
                "userId" to uid,
                "queryText" to cleanQuery,
                "filterSummary" to cleanFilter,
                "title" to cleanTitle,
                "year" to cleanYear,
                "mediaType" to candidate.mediaType.wireValue,
                "country" to cleanCountry,
                "style" to candidate.style.wireValue,
                "languageMode" to candidate.languageMode.wireValue,
                "directorOrCreator" to cleanCreator,
                "whyItMatched" to cleanWhy,
                "iconicCue" to cleanCue,
                "posterUrl" to cleanPoster,
                "personalNote" to cleanNote,
                "sharedToCommunity" to shareToCommunity,
                "communityMemoryId" to communityId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(solvedRef, solvedPayload)

            if (shareToCommunity) {
                val communityPayload = mapOf(
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "status" to "PUBLISHED",
                    "queryText" to cleanQuery,
                    "filterSummary" to cleanFilter,
                    "title" to cleanTitle,
                    "year" to cleanYear,
                    "mediaType" to candidate.mediaType.wireValue,
                    "country" to cleanCountry,
                    "style" to candidate.style.wireValue,
                    "languageMode" to candidate.languageMode.wireValue,
                    "directorOrCreator" to cleanCreator,
                    "whyItMatched" to cleanWhy,
                    "iconicCue" to cleanCue,
                    "posterUrl" to cleanPoster,
                    "yesCount" to 0L,
                    "noCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(commRef, communityPayload)
            }

            batch.commit().await()
            Result.success(solvedRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePersonalNote(
        userId: String,
        memoryId: String,
        note: String
    ): Result<Unit> {
        val docPath = "users/$userId/solved_memories/$memoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memoryId.isNotBlank()) { "Memory ID must not be blank." }
            db.collection("users").document(uid)
                .collection("solved_memories").document(memoryId)
                .update(
                    mapOf(
                        "personalNote" to note.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun shareSolvedMemoryToCommunity(
        userId: String,
        authorName: String,
        memory: SolvedMemory
    ): Result<String> {
        val docPath = "users/$userId/solved_memories/${memory.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memory.id.isNotBlank()) { "Memory ID must not be blank." }
            if (memory.sharedToCommunity && memory.communityMemoryId.isNotBlank()) {
                return Result.success(memory.communityMemoryId)
            }

            val solvedRef = db.collection("users").document(uid)
                .collection("solved_memories").document(memory.id)
            val commRef = db.collection("community_memories").document()

            val batch = db.batch()
            batch.set(
                commRef,
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Cinephile" }.take(80),
                    "status" to "PUBLISHED",
                    "queryText" to memory.queryText.ifBlank { memory.title }.take(500),
                    "filterSummary" to memory.filterSummary.take(200),
                    "title" to memory.title.ifBlank { "Untitled" }.take(200),
                    "year" to memory.year.coerceIn(1880L, 2035L),
                    "mediaType" to memory.mediaEnum().wireValue,
                    "country" to memory.country.ifBlank { "Unknown" }.take(80),
                    "style" to memory.styleEnum().wireValue,
                    "languageMode" to memory.languageEnum().wireValue,
                    "directorOrCreator" to memory.directorOrCreator.take(120),
                    "whyItMatched" to memory.whyItMatched.ifBlank { "Scene match" }.take(1000),
                    "iconicCue" to memory.iconicCue.take(400),
                    "posterUrl" to memory.posterUrl.take(800),
                    "yesCount" to 0L,
                    "noCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                solvedRef,
                mapOf(
                    "sharedToCommunity" to true,
                    "communityMemoryId" to commRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(commRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSolvedMemory(userId: String, memory: SolvedMemory): Result<Unit> {
        val docPath = "users/$userId/solved_memories/${memory.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memory.id.isNotBlank()) { "Memory ID must not be blank." }
            db.collection("users").document(uid)
                .collection("solved_memories").document(memory.id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun castCommunityVote(
        userId: String,
        memoryId: String,
        tappedVote: VoteChoice
    ): Result<Unit> {
        val commPath = "community_memories/$memoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memoryId.isNotBlank()) { "Community memory ID must not be blank." }

            val commRef = db.collection("community_memories").document(memoryId)
            val voteRef = commRef.collection("votes").document(uid)
            val myVoteRef = db.collection("users").document(uid).collection("my_votes").document(memoryId)

            db.runTransaction { transaction ->
                val commSnap = transaction.get(commRef)
                if (!commSnap.exists()) {
                    throw IllegalStateException("Community gem no longer exists.")
                }
                val voteSnap = transaction.get(voteRef)
                val myVoteSnap = transaction.get(myVoteRef)

                val currentVote = if (voteSnap.exists()) {
                    VoteChoice.fromWire(voteSnap.getString("vote"))
                } else {
                    VoteChoice.NONE
                }

                val nextVote = if (currentVote == tappedVote) VoteChoice.NONE else tappedVote
                if (nextVote == currentVote) return@runTransaction

                val currentYes = commSnap.getLong("yesCount") ?: 0L
                val currentNo = commSnap.getLong("noCount") ?: 0L

                var yesDelta = 0L
                var noDelta = 0L
                when (currentVote) {
                    VoteChoice.YES -> yesDelta -= 1L
                    VoteChoice.NO -> noDelta -= 1L
                    VoteChoice.NONE -> Unit
                }
                when (nextVote) {
                    VoteChoice.YES -> yesDelta += 1L
                    VoteChoice.NO -> noDelta += 1L
                    VoteChoice.NONE -> Unit
                }

                val newYes = (currentYes + yesDelta).coerceAtLeast(0L)
                val newNo = (currentNo + noDelta).coerceAtLeast(0L)

                if (voteSnap.exists()) {
                    transaction.update(
                        voteRef,
                        mapOf(
                            "vote" to nextVote.wireValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.set(
                        voteRef,
                        mapOf(
                            "userId" to uid,
                            "memoryId" to memoryId,
                            "vote" to nextVote.wireValue,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (myVoteSnap.exists()) {
                    transaction.update(
                        myVoteRef,
                        mapOf(
                            "vote" to nextVote.wireValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.set(
                        myVoteRef,
                        mapOf(
                            "userId" to uid,
                            "memoryId" to memoryId,
                            "vote" to nextVote.wireValue,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                transaction.update(
                    commRef,
                    mapOf(
                        "yesCount" to newYes,
                        "noCount" to newNo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, commPath)
            Result.failure(e)
        }
    }

    private fun hasFirestoreCause(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }
            .any { it is com.google.firebase.firestore.FirebaseFirestoreException }
    }
}
