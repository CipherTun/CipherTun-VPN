package io.surprise.ciphertun.compose.navigation

data class NewProfileArgs(val importName: String? = null, val importUrl: String? = null, val qrsData: ByteArray? = null)

object ProfileRoutes {
    const val NewProfile = "profile/new"
    const val EditProfile = "profile/edit/{profileId}"
    const val EditProfileBase = "profile/edit"
    const val WizardProtocolPicker = "profile/wizard/protocol?name={name}"
    const val WizardProtocolPickerBase = "profile/wizard/protocol"
    const val WizardCredentialForm = "profile/wizard/credentials/{protocol}?name={name}"
    const val WizardCredentialFormBase = "profile/wizard/credentials"
    const val WizardCustomJson = "profile/wizard/custom?name={name}"
    const val WizardCustomJsonBase = "profile/wizard/custom"

    fun editProfile(profileId: Long): String = "$EditProfileBase/$profileId"
    fun wizardProtocolPicker(name: String = ""): String =
        "$WizardProtocolPickerBase?name=${java.net.URLEncoder.encode(name, "UTF-8")}"
    fun wizardCredentialForm(protocol: String, name: String = ""): String =
        "$WizardCredentialFormBase/$protocol?name=${java.net.URLEncoder.encode(name, "UTF-8")}"
    fun wizardCustomJson(name: String = ""): String =
        "$WizardCustomJsonBase?name=${java.net.URLEncoder.encode(name, "UTF-8")}"
}
