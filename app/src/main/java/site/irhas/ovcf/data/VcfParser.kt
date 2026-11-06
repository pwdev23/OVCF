package site.irhas.ovcf.data

fun parseVcf(content: String): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val vcardBlocks = content.split("BEGIN:VCARD")

    for (block in vcardBlocks) {
        if (block.isBlank()) continue

        var fullName = ""
        var phone = ""
        var email = ""
        var organization = ""
        var note = ""

        block.lines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("FN:", ignoreCase = true) || trimmedLine.startsWith(
                    "FN;",
                    ignoreCase = true
                ) -> {
                    fullName = trimmedLine.substringAfter(":").trim()
                }

                trimmedLine.startsWith("TEL", ignoreCase = true) -> {
                    val value = trimmedLine.substringAfter(":", "").trim()
                    if (value.isNotEmpty()) phone = value
                }

                trimmedLine.startsWith("EMAIL", ignoreCase = true) -> {
                    val value = trimmedLine.substringAfter(":", "").trim()
                    if (value.isNotEmpty()) email = value
                }

                trimmedLine.startsWith("ORG", ignoreCase = true) -> {
                    organization = trimmedLine.substringAfter(":").trim().replace(";", " ")
                }

                trimmedLine.startsWith("NOTE", ignoreCase = true) -> {
                    note = trimmedLine.substringAfter(":").trim()
                }
            }
        }

        if (fullName.isNotEmpty() || phone.isNotEmpty() || email.isNotEmpty()) {
            contacts.add(
                Contact(
                    name = fullName.ifEmpty { "No Name" },
                    phone = phone.ifEmpty { "No Phone" },
                    email = email,
                    organization = organization,
                    note = note
                )
            )
        }
    }
    return contacts
}

fun contactsToVcf(contacts: List<Contact>): String {
    return buildString {
        contacts.forEach { contact ->
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${contact.name}")
            appendLine("TEL:${contact.phone}")
            if (contact.email.isNotEmpty()) {
                appendLine("EMAIL:${contact.email}")
            }
            if (contact.organization.isNotEmpty()) {
                appendLine("ORG:${contact.organization}")
            }
            if (contact.note.isNotEmpty()) {
                appendLine("NOTE:${contact.note}")
            }
            appendLine("END:VCARD")
        }
    }
}
