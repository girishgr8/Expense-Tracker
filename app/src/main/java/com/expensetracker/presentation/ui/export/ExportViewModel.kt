package com.expensetracker.presentation.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.R
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.ExportRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.ExportFilter
import com.expensetracker.domain.model.ExportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsBottomSheet(
    options: List<ExportFilter>,
    onDismiss: () -> Unit,
    onOptionSelected: (ExportFilter) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Export data for...", style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { option ->
                ExportOptionItem(option) {
                    onOptionSelected(option)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ExportOptionItem(
    filter: ExportFilter, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = filter.displayName)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun getFileIcon(mimeType: String?): ImageVector {
    return when {
        mimeType?.uppercase()!!
            .contains(ExportFormat.PDF.displayName()) -> Icons.Default.PictureAsPdf

        mimeType.uppercase().contains(ExportFormat.CSV.displayName()) -> Icons.Default.TableChart
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun openFile(context: Context, uri: Uri) {
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {

        // 🔥 Fallback → Files app
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(
            Intent.createChooser(fallbackIntent, "Open file")
        )
    }
}

@Composable
fun ExportSuccessBottomSheet(
    uri: Uri, onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 🔹 Extract file name from URI
    val fileName = remember(uri) {
        var name = "Exported file"

        val cursor = context.contentResolver.query(
            uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        name
    }
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {

                // 🔹 HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Export Successful",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 🔹 DESCRIPTION
                Text(
                    text = "The exported file has been saved to the \"Downloads\" folder on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 FILE PREVIEW (NEW)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = getFileIcon(mimeType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        // 📂 OPEN BUTTON
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier
                                .clickable { openFile(context, uri) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open")
                            }
                        }

                        // 📤 SHARE BUTTON
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = mimeType
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent, "Share file"
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatBottomSheet(
    onDismiss: () -> Unit, onFormatSelected: (ExportFormat) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔹 TITLE
            Text(
                text = "Export data to...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 SUBTITLE
            Text(
                text = "Choose a format to export your transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 OPTIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                ExportFormatItem(
                    title = ExportFormat.CSV.displayName(),
                    icon = R.drawable.ic_csv_logo,
                    modifier = Modifier.weight(1f)
                ) {
                    onFormatSelected(ExportFormat.CSV)
                }

                ExportFormatItem(
                    title = ExportFormat.PDF.displayName(),
                    icon = R.drawable.ic_pdf_logo,
                    modifier = Modifier.weight(1f)
                ) {
                    onFormatSelected(ExportFormat.PDF)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ExportFormatItem(
    title: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp) // ✅ exact height like screenshot
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp // ✅ subtle shadow (not heavy)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class ExportResult(
    val uri: Uri? = null, val isSuccess: Boolean = false
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val transactionRepository: TransactionRepository,
    authManager: AuthManager
) : ViewModel() {
    var selectedFilter: ExportFilter = ExportFilter.AllTime
    private val _exportResult = MutableStateFlow(ExportResult())
    val exportResult = _exportResult.asStateFlow()

    val userId = authManager.userId
    var isExporting by mutableStateOf(false)

    private val _filters = MutableStateFlow<List<ExportFilter>>(emptyList())
    val filters = _filters.asStateFlow()

    fun loadFilters() {
        viewModelScope.launch {
            _filters.value = transactionRepository.getAvailableFilters(userId)
        }
    }

    fun setFilter(filter: ExportFilter) {
        selectedFilter = filter
    }

    fun export(context: Context, userName: String, userEmail: String, isPdf: Boolean) {
        viewModelScope.launch {
            isExporting = true
            val uri = exportRepository.exportTransactions(
                context = context,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                filter = selectedFilter,
                isPdf = isPdf
            )
            isExporting = false
            _exportResult.value = ExportResult(uri, uri != null)
        }
    }

    fun resetExportState() {
        _exportResult.value = ExportResult()
    }
}