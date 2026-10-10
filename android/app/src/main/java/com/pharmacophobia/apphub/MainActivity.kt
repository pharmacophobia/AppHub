package com.pharmacophobia.apphub

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var installer: AppInstaller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installer = AppInstaller(this)

        setContent {
            AppHubTheme {
                AppHubScreen(installer = installer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-triggers UI refresh when returning from package installer
    }
}

@Composable
fun AppHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF181825),
            surface = Color(0xFF1E1E2E),
            primary = Color(0xFF89B4FA),
            secondary = Color(0xFFA6E3A1),
            tertiary = Color(0xFFFAB387),
            onBackground = Color(0xFFCDD6F4),
            onSurface = Color(0xFFCDD6F4)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHubScreen(installer: AppInstaller) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Map of app ID to downloading progress percentage (0..100) or null
    val downloadProgressMap = remember { mutableStateMapOf<String, Int>() }

    fun refreshCatalog() {
        isLoading = true
        scope.launch {
            apps = AppRepository.loadApps(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCatalog()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF89B4FA).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🚀", fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AppHub", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Personal Android App Store", fontSize = 11.sp, color = Color(0xFFA6ADC8))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { refreshCatalog() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Catalog", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFA6ADC8)) },
                        placeholder = { Text("Search apps, categories, tags...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF45475A)
                        )
                    )

                    // Category Filter Chips
                    val categories = remember(apps) {
                        listOf("All") + apps.map { it.category }.distinct()
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = Color(0xFFA6ADC8)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Filtered list
                    val filtered = remember(apps, searchQuery, selectedCategory) {
                        apps.filter { app ->
                            val matchesCat = selectedCategory == "All" || app.category == selectedCategory
                            val q = searchQuery.trim().lowercase()
                            val matchesQuery = q.isEmpty() ||
                                app.name.lowercase().contains(q) ||
                                app.description.lowercase().contains(q) ||
                                app.tags.any { it.lowercase().contains(q) }
                            matchesCat && matchesQuery
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No apps found matching your search.", color = Color(0xFFA6ADC8))
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items = filtered, key = { it.id }) { app ->
                                val downloadPct = downloadProgressMap[app.id]

                                AppCard(
                                    app = app,
                                    downloadProgress = downloadPct,
                                    onInstallOrUpdate = {
                                        if (downloadPct != null) return@AppCard
                                        downloadProgressMap[app.id] = 0
                                        scope.launch {
                                            val file = installer.downloadApk(app) { pct ->
                                                downloadProgressMap[app.id] = pct
                                            }
                                            downloadProgressMap.remove(app.id)
                                            if (file != null && file.exists()) {
                                                installer.installApk(file)
                                                // Trigger refresh after returning
                                                refreshCatalog()
                                            } else {
                                                Toast.makeText(context, "Download failed. Check connection.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onOpen = {
                                        installer.launchApp(app.packageName)
                                    },
                                    onOpenRepo = {
                                        installer.openBrowserLink("https://github.com/${app.githubRepo}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppCard(
    app: AppItem,
    downloadProgress: Int?, // null if not downloading, 0..100 if downloading
    onInstallOrUpdate: () -> Unit,
    onOpen: () -> Unit,
    onOpenRepo: () -> Unit
) {
    val isDownloading = downloadProgress != null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon, Title, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF313244),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(app.icon, fontSize = 28.sp)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFFCDD6F4)
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                app.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                "v${app.version}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (app.isInstalled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (app.hasUpdate) Color(0xFFFAB387).copy(alpha = 0.2f) else Color(0xFF45475A)
                            ) {
                                Text(
                                    if (app.hasUpdate) "Update Available" else "Installed",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (app.hasUpdate) Color(0xFFFAB387) else Color(0xFFA6ADC8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onOpenRepo) {
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = "GitHub Repo",
                        tint = Color(0xFF6C7086),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Description
            Text(
                app.description,
                fontSize = 13.sp,
                color = Color(0xFFA6ADC8),
                lineHeight = 18.sp
            )

            // Tags
            if (app.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    app.tags.take(3).forEach { tag ->
                        Text(
                            "#$tag",
                            fontSize = 11.sp,
                            color = Color(0xFF6C7086)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Download Progress Bar if active
            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (downloadProgress!! >= 100) "Opening Package Installer..." else "Downloading APK...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "$downloadProgress%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.isInstalled && !app.hasUpdate) {
                        Button(
                            onClick = onOpen,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF313244))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open", color = Color(0xFFCDD6F4), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onInstallOrUpdate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                app.hasUpdate -> Color(0xFFFAB387)
                                app.isInstalled -> Color(0xFF313244)
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            contentColor = when {
                                app.hasUpdate -> Color(0xFF11111B)
                                app.isInstalled -> Color(0xFFCDD6F4)
                                else -> Color(0xFF11111B)
                            }
                        )
                    ) {
                        Icon(
                            if (app.hasUpdate) Icons.Default.SystemUpdate else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                app.hasUpdate -> "Update (v${app.version})"
                                app.isInstalled -> "Reinstall"
                                else -> "Install"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
