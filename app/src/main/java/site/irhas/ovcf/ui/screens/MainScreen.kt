package site.irhas.ovcf.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import site.irhas.ovcf.R
import site.irhas.ovcf.ui.components.ContactDetailBottomSheet
import site.irhas.ovcf.ui.components.ContactItem
import site.irhas.ovcf.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    initialUri: Uri? = null
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = viewModel.selectionMode) {
        viewModel.clearSelection()
    }

    val view = LocalView.current
    LaunchedEffect(viewModel.selectionMode) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (viewModel.selectionMode) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { viewModel.loadVcfContent(context, it) }
    }

    val pickVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadVcfContent(context, it) }
    }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportContacts(context, it)
        }
    }

    if (viewModel.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.updateShowResetDialog(false) },
            title = { Text(stringResource(R.string.confirm_reset)) },
            text = { Text(stringResource(R.string.confirm_reset_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetData() }) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowResetDialog(false) }) {
                    Text(stringResource(R.string.cancel))
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
                        label = { Text(stringResource(R.string.privacy_policy)) },
                        selected = false,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://irhas.site/app/open-vcf#privacy".toUri()
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
                        text = "${stringResource(R.string.version)} $appVersion",
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
                        if (viewModel.contacts.isNotEmpty()) {
                            if (viewModel.selectionMode) {
                                Text(
                                    stringResource(
                                        R.string.selected_contacts,
                                        viewModel.selectedContacts.size
                                    )
                                )
                            } else {
                                Column {
                                    Text(
                                        stringResource(R.string.contacts, viewModel.contacts.size),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    viewModel.fileName?.let {
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
                            Text(stringResource(R.string.app_name))
                        }
                    },
                    navigationIcon = {
                        if (viewModel.contacts.isEmpty()) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu)
                                )
                            }
                        }
                    },
                    actions = {
                        if (viewModel.contacts.isNotEmpty()) {
                            if (viewModel.selectionMode) {
                                TextButton(onClick = { viewModel.toggleSelectAll() }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stringResource(R.string.select_all),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Checkbox(
                                            checked = viewModel.isAllSelected,
                                            onCheckedChange = null
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { viewModel.updateShowResetDialog(true) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.reset)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (viewModel.contacts.isEmpty()) {
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
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.pick_vcf_file)
                        )
                    }
                }
            },
            bottomBar = {
                if (viewModel.selectionMode) {
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
                        Text(stringResource(R.string.export_vcf))
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
                if (viewModel.contacts.isNotEmpty()) {
                    PrimaryTabRow(selectedTabIndex = viewModel.selectedTabIndex) {
                        Tab(
                            selected = viewModel.selectedTabIndex == 0,
                            onClick = { viewModel.updateSelectedTabIndex(0) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.people,
                                        viewModel.personContacts.size
                                    )
                                )
                            }
                        )
                        Tab(
                            selected = viewModel.selectedTabIndex == 1,
                            onClick = { viewModel.updateSelectedTabIndex(1) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.others,
                                        viewModel.otherContacts.size
                                    )
                                )
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(viewModel.displayList) { contact ->
                            ContactItem(
                                contact = contact,
                                isSelected = viewModel.selectedContacts.contains(contact),
                                selectionMode = viewModel.selectionMode,
                                onToggle = { viewModel.toggleContactSelection(contact) },
                                onLongClick = {
                                    if (!viewModel.selectionMode) {
                                        viewModel.toggleContactSelection(contact)
                                    }
                                },
                                onClick = {
                                    if (viewModel.selectionMode) {
                                        viewModel.toggleContactSelection(contact)
                                    } else if (viewModel.selectedTabIndex == 0) {
                                        viewModel.updateSelectedContactForDetails(contact)
                                    }
                                }
                            )
                        }
                    }
                } else if (viewModel.vcfContent.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = viewModel.vcfContent)
                    }
                    Button(onClick = {
                        viewModel.clearFileContent()
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.back))
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (viewModel.selectedContactForDetails != null) {
        ContactDetailBottomSheet(
            contact = viewModel.selectedContactForDetails!!,
            sheetState = sheetState,
            onDismiss = { viewModel.updateSelectedContactForDetails(null) },
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }
}
