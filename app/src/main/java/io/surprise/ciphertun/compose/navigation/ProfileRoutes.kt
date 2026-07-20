package io.surprise.ciphertun.compose.navigation

data class NewProfileArgs(val importName: String? = null, val importUrl: String? = null, val qrsData: ByteArray? = null)

object ProfileRoutes {
    const val NewProfile = "profile/new"
    const val EditProfile = "profile/edit/{profileId}"
    const val EditProfileBase = "profile/edit"
    const val Wizard = "profile/wizard"
    const val WizardForm = "profile/wizard/{protocol}"

    fun editProfile(profileId: Long): String = "$EditProfileBase/$profileId"
    fun wizardForm(protocol: String): String = "profile/wizard/$protocol"
}
