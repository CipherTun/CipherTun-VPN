package io.surprise.ciphertun.config

import android.content.Context
import io.nekohasekai.libbox.Libbox
import io.surprise.ciphertun.database.Profile
import io.surprise.ciphertun.database.ProfileManager
import io.surprise.ciphertun.database.TypedProfile
import java.io.File

object ProfileWizardRepository {

    suspend fun saveProfile(context: Context, outboundProfile: OutboundProfile): Profile {
        val typedProfile = TypedProfile().apply {
            type = TypedProfile.Type.Local
        }

        val profile = Profile(name = outboundProfile.remark, typed = typedProfile).apply {
            userOrder = ProfileManager.nextOrder()
        }

        val fileID = ProfileManager.nextFileID()
        val configDirectory = File(context.filesDir, "configs").also { it.mkdirs() }
        val configFile = File(configDirectory, "$fileID.json")
        typedProfile.path = configFile.path

        val configContent = SingBoxConfigFactory.build(outboundProfile)
        Libbox.checkConfig(configContent)
        configFile.writeText(configContent)

        ProfileManager.create(profile, andSelect = true)
        return profile
    }

    /**
     * Saves a hand-written/pasted sing-box config exactly as given — the
     * "Custom (JSON)" wizard entry point. Still runs it through
     * Libbox.checkConfig so a broken config is rejected before it's saved,
     * same guarantee as the guided protocol forms get.
     */
    suspend fun saveRawConfig(context: Context, name: String, rawJson: String): Profile {
        Libbox.checkConfig(rawJson)

        val typedProfile = TypedProfile().apply {
            type = TypedProfile.Type.Local
        }

        val profile = Profile(name = name, typed = typedProfile).apply {
            userOrder = ProfileManager.nextOrder()
        }

        val fileID = ProfileManager.nextFileID()
        val configDirectory = File(context.filesDir, "configs").also { it.mkdirs() }
        val configFile = File(configDirectory, "$fileID.json")
        typedProfile.path = configFile.path
        configFile.writeText(rawJson)

        ProfileManager.create(profile, andSelect = true)
        return profile
    }
}
