package site.irhas.ovcf.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import site.irhas.ovcf.R
import site.irhas.ovcf.data.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailBottomSheet(
    contact: Contact,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.contact_details),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ListItem(
                headlineContent = { Text(contact.name) },
                overlineContent = { Text(stringResource(R.string.name)) },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text(contact.phone) },
                overlineContent = { Text(stringResource(R.string.phone)) },
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
                    overlineContent = { Text(stringResource(R.string.email)) },
                    leadingContent = { Icon(Icons.Default.Email, contentDescription = null) }
                )
            }

            if (contact.organization.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text(contact.organization) },
                    overlineContent = { Text(stringResource(R.string.organization)) },
                    leadingContent = { Icon(Icons.Default.Work, contentDescription = null) }
                )
            }

            if (contact.note.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text(contact.note) },
                    overlineContent = { Text(stringResource(R.string.note)) },
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
