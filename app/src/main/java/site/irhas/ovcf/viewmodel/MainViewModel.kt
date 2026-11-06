package site.irhas.ovcf.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import site.irhas.ovcf.data.Contact
import site.irhas.ovcf.data.contactsToVcf
import site.irhas.ovcf.data.parseVcf
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel : ViewModel() {
    var vcfContent by mutableStateOf("")
        private set
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var fileName by mutableStateOf<String?>(null)
        private set
    var selectedTabIndex by mutableIntStateOf(0)
        private set
    var showResetDialog by mutableStateOf(false)
        private set
    var selectedContacts by mutableStateOf(setOf<Contact>())
        private set
    var selectedContactForDetails by mutableStateOf<Contact?>(null)
        private set

    val selectionMode: Boolean
        get() = selectedContacts.isNotEmpty()

    val personContacts: List<Contact>
        get() = contacts.filter { contact ->
            val digitsOnly = contact.phone.filter { it.isDigit() }
            digitsOnly.length >= 7 || contact.email.isNotEmpty()
        }

    val otherContacts: List<Contact>
        get() = contacts.filter { contact ->
            val digitsOnly = contact.phone.filter { it.isDigit() }
            (digitsOnly.isNotEmpty() && digitsOnly.length < 7 && contact.email.isEmpty()) ||
                    (digitsOnly.isEmpty() && contact.email.isEmpty())
        }

    val displayList: List<Contact>
        get() = if (selectedTabIndex == 0) personContacts else otherContacts

    val isAllSelected: Boolean
        get() = displayList.isNotEmpty() && selectedContacts.containsAll(displayList)

    fun updateSelectedTabIndex(index: Int) {
        selectedTabIndex = index
        selectedContacts = emptySet()
    }

    fun updateShowResetDialog(show: Boolean) {
        showResetDialog = show
    }

    fun updateSelectedContactForDetails(contact: Contact?) {
        selectedContactForDetails = contact
    }

    fun clearSelection() {
        selectedContacts = emptySet()
    }

    fun toggleContactSelection(contact: Contact) {
        selectedContacts = if (selectedContacts.contains(contact)) {
            selectedContacts - contact
        } else {
            selectedContacts + contact
        }
    }

    fun toggleSelectAll() {
        selectedContacts = if (isAllSelected) {
            selectedContacts - displayList.toSet()
        } else {
            selectedContacts + displayList.toSet()
        }
    }

    fun resetData() {
        vcfContent = ""
        contacts = emptyList()
        fileName = null
        selectedTabIndex = 0
        showResetDialog = false
        selectedContacts = emptySet()
        selectedContactForDetails = null
    }

    fun clearFileContent() {
        vcfContent = ""
        fileName = null
    }

    fun loadVcfContent(context: Context, uri: Uri) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        fileName = cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) it.getString(displayNameIndex) else null
            } else null
        } ?: uri.path?.substringAfterLast('/')

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                vcfContent = content
                contacts = parseVcf(content)
            }
        } catch (e: Exception) {
            vcfContent = "Error reading file: ${e.message}"
            contacts = emptyList()
        }
    }

    fun exportContacts(context: Context, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val toExport =
                    if (selectedContacts.isNotEmpty()) selectedContacts.toList() else contacts
                outputStream.write(contactsToVcf(toExport).toByteArray())
            }
            selectedContacts = emptySet()
        } catch (e: Exception) {
            // Handle export error if needed
        }
    }
}
