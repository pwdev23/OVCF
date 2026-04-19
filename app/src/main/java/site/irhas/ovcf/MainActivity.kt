package site.irhas.ovcf

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import site.irhas.ovcf.ui.theme.OVCFTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OVCFTheme {
                VcfEditorApp(intent?.data)
            }
        }
    }
}

data class Contact(
    val name: String,
    val phone: String,
    val email: String,
    val organization: String = "",
    val note: String = ""
)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: Contact,
    isSelected: Boolean,
    selectionMode: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (contact.email.isNotEmpty()) {
                    Text(text = contact.email, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VcfEditorApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var vcfContent by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedContacts by remember { mutableStateOf(setOf<Contact>()) }
    var selectedContactForDetails by remember { mutableStateOf<Contact?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectionMode by remember {
        derivedStateOf { selectedContacts.isNotEmpty() }
    }

    val personContacts by remember {
        derivedStateOf {
            contacts.filter { contact ->
                val digitsOnly = contact.phone.filter { it.isDigit() }
                digitsOnly.length >= 7 || contact.email.isNotEmpty()
            }
        }
    }

    val otherContacts by remember {
        derivedStateOf {
            contacts.filter { contact ->
                val digitsOnly = contact.phone.filter { it.isDigit() }
                (digitsOnly.isNotEmpty() && digitsOnly.length < 7 && contact.email.isEmpty()) ||
                        (digitsOnly.isEmpty() && contact.email.isEmpty())
            }
        }
    }

    val displayList by remember {
        derivedStateOf { if (selectedTabIndex == 0) personContacts else otherContacts }
    }

    val isAllSelected by remember {
        derivedStateOf {
            displayList.isNotEmpty() && selectedContacts.containsAll(displayList)
        }
    }

    val view = LocalView.current
    LaunchedEffect(selectionMode) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (selectionMode) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    LaunchedEffect(selectedTabIndex) {
        selectedContacts = emptySet()
    }

    fun loadVcfContent(uri: Uri) {
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

    LaunchedEffect(initialUri) {
        initialUri?.let { loadVcfContent(it) }
    }

    val pickVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadVcfContent(it) }
    }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val toExport =
                        if (selectedContacts.isNotEmpty()) selectedContacts.toList() else contacts
                    outputStream.write(contactsToVcf(toExport).toByteArray())
                }
                selectedContacts = emptySet()
            } catch (e: Exception) {
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Reset") },
            text = { Text("Are you sure you want to clear the current VCF file? This will return you to the start.") },
            confirmButton = {
                TextButton(onClick = {
                    vcfContent = ""
                    contacts = emptyList()
                    fileName = null
                    selectedTabIndex = 0
                    showResetDialog = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val appVersion = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(304.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    NavigationDrawerItem(
                        label = { Text("Privacy policy") },
                        selected = false,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://irhas.site/app/ovfc#privacy".toUri()
                            )
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Version $appVersion",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (contacts.isNotEmpty()) {
                            if (selectionMode) {
                                Text(text = "${selectedContacts.size} Selected")
                            } else {
                                Column {
                                    Text(
                                        text = "${contacts.size} Contacts",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    fileName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("OVCF")
                        }
                    },
                    navigationIcon = {
                        if (contacts.isEmpty()) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        if (contacts.isNotEmpty()) {
                            if (selectionMode) {
                                TextButton(onClick = {
                                    selectedContacts = if (isAllSelected) {
                                        selectedContacts - displayList.toSet()
                                    } else {
                                        selectedContacts + displayList.toSet()
                                    }
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Select all", modifier = Modifier.padding(end = 8.dp))
                                        Checkbox(
                                            checked = isAllSelected,
                                            onCheckedChange = null
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { showResetDialog = true }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (contacts.isEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            pickVcfLauncher.launch(
                                arrayOf(
                                    "text/vcard",
                                    "text/x-vcard",
                                    "text/directory"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Pick VCF File")
                    }
                }
            },
            bottomBar = {
                if (selectionMode) {
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat(
                                "yyyyMMdd_HHmmss",
                                Locale.getDefault()
                            ).format(Date())
                            exportVcfLauncher.launch("contacts_$timestamp.vcf")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RectangleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Export VCF")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                if (contacts.isNotEmpty()) {
                    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("People (${personContacts.size})") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Others (${otherContacts.size})") }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayList) { contact ->
                            ContactItem(
                                contact = contact,
                                isSelected = selectedContacts.contains(contact),
                                selectionMode = selectionMode,
                                onToggle = {
                                    selectedContacts = if (selectedContacts.contains(contact)) {
                                        selectedContacts - contact
                                    } else {
                                        selectedContacts + contact
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectedContacts = selectedContacts + contact
                                    }
                                },
                                onClick = {
                                    if (selectionMode) {
                                        selectedContacts = if (selectedContacts.contains(contact)) {
                                            selectedContacts - contact
                                        } else {
                                            selectedContacts + contact
                                        }
                                    } else if (selectedTabIndex == 0) {
                                        selectedContactForDetails = contact
                                    }
                                }
                            )
                        }
                    }
                } else if (vcfContent.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = vcfContent)
                    }
                    Button(onClick = {
                        vcfContent = ""
                        fileName = null
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Back")
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap the + button to load a VCF file",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (selectedContactForDetails != null) {
        val contact = selectedContactForDetails!!
        ModalBottomSheet(
            onDismissRequest = { selectedContactForDetails = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Contact Details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text(contact.name) },
                    overlineContent = { Text("Name") },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                ListItem(
                    headlineContent = { Text(contact.phone) },
                    overlineContent = { Text("Phone") },
                    leadingContent = { Icon(Icons.Default.Phone, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("phone", contact.phone)
                            clipboard.setPrimaryClip(clip)
                            scope.launch {
                                snackbarHostState.showSnackbar("Phone number copied to clipboard")
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy phone")
                        }
                    }
                )

                if (contact.email.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(contact.email) },
                        overlineContent = { Text("Email") },
                        leadingContent = { Icon(Icons.Default.Email, contentDescription = null) }
                    )
                }

                if (contact.organization.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(contact.organization) },
                        overlineContent = { Text("Organization") },
                        leadingContent = { Icon(Icons.Default.Work, contentDescription = null) }
                    )
                }

                if (contact.note.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(contact.note) },
                        overlineContent = { Text("Note") },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}