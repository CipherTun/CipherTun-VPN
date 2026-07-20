package io.surprise.ciphertun.config

import io.surprise.ciphertun.database.Profile
import io.surprise.ciphertun.database.ProfileManager
import io.surprise.ciphertun.database.TypedProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object ProfileRepository {

    private fun observeAllProfiles(): Flow<List<Profile>> = callbackFlow {
        suspend fun push() {
            trySend(ProfileManager.list())
        }
        push()
        val callback: () -> Unit = {
            launch { push() }
        }
        ProfileManager.registerCallback(callback)
        awaitClose { ProfileManager.unregisterCallback(callback) }
    }

    fun observeLocalProfiles(): Flow<List<Profile>> =
        observeAllProfiles().map { profiles ->
            profiles.filter { it.typed.type == TypedProfile.Type.Local }
        }

    fun observeRemoteProfiles(): Flow<List<Profile>> =
        observeAllProfiles().map { profiles ->
            profiles.filter { it.typed.type == TypedProfile.Type.Remote }
        }
}
