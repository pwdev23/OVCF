package site.irhas.ovcf.data

data class Contact(
    val name: String,
    val phone: String,
    val email: String,
    val organization: String = "",
    val note: String = ""
)
