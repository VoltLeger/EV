package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.CarExpense
import com.example.data.model.ChargingSession
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ShareSessionCardDialog
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.getCategoryEmoji
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import java.util.Locale

enum class HistoryTab {
    CHARGING, EXPENSES
}

enum class HistorySortOption {
    DATE_DESC,
    DATE_ASC,
    ODOMETER_DESC,
    ODOMETER_ASC,
    COST_DESC,
    KWH_DESC
}

@Composable
fun HistoryScreen(
    sessions: List<ChargingSession>,
    onUpdateSession: (ChargingSession) -> Unit,
    onDeleteSession: (ChargingSession) -> Unit,
    expenses: List<CarExpense> = emptyList(),
    onAddExpense: ((category: String, amount: Double, odometer: Double?, comment: String?) -> Unit)? = null,
    onUpdateExpense: ((CarExpense) -> Unit)? = null,
    onDeleteExpense: ((CarExpense) -> Unit)? = null,
    topCategories: List<String> = emptyList(),
    defaultOdometer: Double? = null,
    activeCar: Car? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    var currentTab by remember { mutableStateOf(HistoryTab.CHARGING) }

    // Charging session states
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(HistorySortOption.DATE_DESC) }
    var editingSession by remember { mutableStateOf<ChargingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChargingSession?>(null) }
    var sessionToShare by remember { mutableStateOf<ChargingSession?>(null) }

    // Expenses states
    var expenseSearchQuery by remember { mutableStateOf("") }
    var selectedExpenseCategory by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<CarExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<CarExpense?>(null) }

    // Filter sessions
    val filtered = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            sessions.filter {
                it.operatorName.lowercase(Locale.getDefault()).contains(q) ||
                        it.stationType.lowercase(Locale.getDefault()).contains(q) ||
                        (it.operatorComment?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                        it.startOdometer.toInt().toString().contains(q)
            }
        }
    }

    // Sort sessions
    val sortedSessions = remember(filtered, selectedSort) {
        when (selectedSort) {
            HistorySortOption.DATE_DESC -> filtered.sortedByDescending { it.startTime }
            HistorySortOption.DATE_ASC -> filtered.sortedBy { it.startTime }
            HistorySortOption.ODOMETER_DESC -> filtered.sortedByDescending { it.startOdometer }
            HistorySortOption.ODOMETER_ASC -> filtered.sortedBy { it.startOdometer }
            HistorySortOption.COST_DESC -> filtered.sortedByDescending { it.totalCost }
            HistorySortOption.KWH_DESC -> filtered.sortedByDescending { it.kwhDeliveredByStation }
        }
    }

    // Charging totals
    val totalCost = remember(filtered) { filtered.sumOf { it.totalCost } }
    val totalKwh = remember(filtered) { filtered.sumOf { it.kwhDeliveredByStation } }
    val minOdo = remember(filtered) { filtered.minOfOrNull { it.startOdometer } ?: 0.0 }
    val maxOdo = remember(filtered) { filtered.maxOfOrNull { it.startOdometer } ?: 0.0 }
    val coveredKm = (maxOdo - minOdo).coerceAtLeast(0.0)

    // Filter expenses
    val filteredExpenses = remember(expenses, expenseSearchQuery, selectedExpenseCategory) {
        expenses.filter { exp ->
            val matchesCategory = selectedExpenseCategory == null || exp.category.equals(selectedExpenseCategory, ignoreCase = true)
            val matchesSearch = if (expenseSearchQuery.isBlank()) true else {
                val q = expenseSearchQuery.trim().lowercase(Locale.getDefault())
                exp.category.lowercase(Locale.getDefault()).contains(q) ||
                        (exp.comment?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                        (exp.odometer != null && exp.odometer.toInt().toString().contains(q))
            }
            matchesCategory && matchesSearch
        }.sortedByDescending { it.timestamp }
    }

    val totalExpensesCost = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    LiquidGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentTab == HistoryTab.CHARGING) Icons.Default.History else Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentTab == HistoryTab.CHARGING) "История зарядок" else "Прочие траты",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (currentTab == HistoryTab.CHARGING) "Всего ${filtered.size} сессий" else "Всего ${filteredExpenses.size} записей",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentTab == HistoryTab.EXPENSES) {
                            IconButton(
                                onClick = { showAddExpenseDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить расход", tint = ElectricCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Tabs: ⚡ Зарядки | 🔧 Прочие траты
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = currentTab == HistoryTab.CHARGING,
                            onClick = { currentTab = HistoryTab.CHARGING },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = "⚡ Зарядки (${sessions.size})",
                                fontSize = 13.sp,
                                fontWeight = if (currentTab == HistoryTab.CHARGING) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        SegmentedButton(
                            selected = currentTab == HistoryTab.EXPENSES,
                            onClick = { currentTab = HistoryTab.EXPENSES },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = "🔧 Прочие траты (${expenses.size})",
                                fontSize = 13.sp,
                                fontWeight = if (currentTab == HistoryTab.EXPENSES) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ==================== TAB 1: CHARGING SESSIONS ====================
            if (currentTab == HistoryTab.CHARGING) {
                // Summary Totals Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        VoltCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_totals_card"),
                            borderColor = ElectricCyan.copy(alpha = 0.35f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "ИТОГИ ПО ВЫБРАННЫМ ЗАРЯДКАМ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SoftBlue,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Total Cost
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Сумма расходов", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", totalCost)} $currency",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }

                                    // Total Delivered kWh
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Всего энергии", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", totalKwh)} кВт·ч",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Covered Distance
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Охват пробега", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${coveredKm.toInt()} км",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BatteryGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input"),
                            placeholder = { Text("Поиск по оператору, типу или пробегу...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = SoftBlue)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Sort Options Chips
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.DATE_DESC,
                                onClick = { selectedSort = HistorySortOption.DATE_DESC },
                                label = { Text("Сначала новые", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.DATE_ASC,
                                onClick = { selectedSort = HistorySortOption.DATE_ASC },
                                label = { Text("Сначала старые", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.ODOMETER_DESC,
                                onClick = { selectedSort = HistorySortOption.ODOMETER_DESC },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text("По пробегу (км) ↓", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.ODOMETER_ASC,
                                onClick = { selectedSort = HistorySortOption.ODOMETER_ASC },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text("По пробегу (км) ↑", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.COST_DESC,
                                onClick = { selectedSort = HistorySortOption.COST_DESC },
                                label = { Text("По стоимости", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSort == HistorySortOption.KWH_DESC,
                                onClick = { selectedSort = HistorySortOption.KWH_DESC },
                                label = { Text("По кВт·ч", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                // Sessions List
                if (sortedSessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Зарядок по вашему запросу не найдено" else "История зарядок пуста",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(sortedSessions) { session ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                            HistorySessionItemCard(
                                session = session,
                                currency = currency,
                                onClick = { editingSession = session }
                            )
                        }
                    }
                }
            }

            // ==================== TAB 2: CAR EXPENSES ====================
            if (currentTab == HistoryTab.EXPENSES) {
                // Summary Card for Expenses
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        VoltCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expenses_totals_card"),
                            borderColor = SoftBlue.copy(alpha = 0.35f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ИТОГИ ПО ПРОЧИМ ЗАТРАТАМ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SoftBlue,
                                        letterSpacing = 0.8.sp
                                    )
                                    Button(
                                        onClick = { showAddExpenseDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Добавить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Всего потрачено", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", totalExpensesCost)} $currency",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("Количество записей", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${filteredExpenses.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedExpenseCategory == null,
                                onClick = { selectedExpenseCategory = null },
                                label = { Text("Все категории", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }

                        val catsToDisplay = (topCategories.take(5) + listOf("Мойка", "ТО", "Страховка", "Шиномонтаж", "Парковка")).distinct()
                        items(catsToDisplay) { cat ->
                            val emoji = getCategoryEmoji(cat)
                            val isSelected = selectedExpenseCategory?.equals(cat, ignoreCase = true) == true
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedExpenseCategory = if (isSelected) null else cat
                                },
                                label = { Text("$emoji $cat", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                // Search Bar for expenses
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = expenseSearchQuery,
                            onValueChange = { expenseSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_search_input"),
                            placeholder = { Text("Поиск по категории, заметке...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = SoftBlue)
                            },
                            trailingIcon = {
                                if (expenseSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { expenseSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Expenses List
                if (filteredExpenses.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (expenseSearchQuery.isNotEmpty() || selectedExpenseCategory != null)
                                    "Расходов по выбранному фильтру не найдено"
                                else
                                    "В журнале пока нет прочих расходов",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddExpenseDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить первый расход")
                            }
                        }
                    }
                } else {
                    items(filteredExpenses) { expense ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                            HistoryExpenseItemCard(
                                expense = expense,
                                currency = currency,
                                onEdit = { editingExpense = expense },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== DIALOGS ====================

    // 1. Edit Session Dialog
    if (editingSession != null) {
        val s = editingSession!!
        var editOperatorName by remember(s.id) { mutableStateOf(s.operatorName) }
        var editStartSoc by remember(s.id) { mutableStateOf(s.startSoc.toInt().toString()) }
        var editEndSoc by remember(s.id) { mutableStateOf(s.endSoc.toInt().toString()) }
        var editKwh by remember(s.id) { mutableStateOf(String.format(Locale.US, "%.1f", s.kwhDeliveredByStation)) }
        var editCost by remember(s.id) { mutableStateOf(String.format(Locale.US, "%.2f", s.totalCost)) }
        var editOdometer by remember(s.id) { mutableStateOf(s.startOdometer.toInt().toString()) }
        var editStationType by remember(s.id) { mutableStateOf(s.stationType) }
        var editComment by remember(s.id) { mutableStateOf(s.operatorComment ?: "") }

        AlertDialog(
            onDismissRequest = { editingSession = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Редактирование зарядки", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            val toShare = s
                            editingSession = null
                            sessionToShare = toShare
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = ElectricCyan)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editOperatorName,
                        onValueChange = { editOperatorName = it },
                        label = { Text("Оператор / Станция") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editStartSoc,
                            onValueChange = { editStartSoc = it },
                            label = { Text("Начальный %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editEndSoc,
                            onValueChange = { editEndSoc = it },
                            label = { Text("Конечный %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editKwh,
                            onValueChange = { editKwh = it },
                            label = { Text("Заряжено кВт·ч") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCost,
                            onValueChange = { editCost = it },
                            label = { Text("Сумма ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editOdometer,
                        onValueChange = { editOdometer = it },
                        label = { Text("Пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editComment,
                        onValueChange = { editComment = it },
                        label = { Text("Заметка / Тег") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = s.copy(
                            operatorName = editOperatorName.trim(),
                            startSoc = editStartSoc.toDoubleOrNull() ?: s.startSoc,
                            endSoc = editEndSoc.toDoubleOrNull() ?: s.endSoc,
                            kwhDeliveredByStation = editKwh.toDoubleOrNull() ?: s.kwhDeliveredByStation,
                            totalCost = editCost.toDoubleOrNull() ?: s.totalCost,
                            startOdometer = editOdometer.toDoubleOrNull() ?: s.startOdometer,
                            stationType = editStationType,
                            operatorComment = editComment.trim().ifEmpty { null }
                        )
                        onUpdateSession(updated)
                        editingSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val toDelete = s
                            editingSession = null
                            sessionToDelete = toDelete
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                    TextButton(onClick = { editingSession = null }) {
                        Text(strings.cancel)
                    }
                }
            }
        )
    }

    // 2. Confirm Delete Session Dialog
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить эту зарядку?") },
            text = {
                Text("Зарядка на ${s.operatorName.ifEmpty { "EV Station" }} (+${String.format(Locale.US, "%.1f", s.kwhDeliveredByStation)} кВт·ч, ${formatCurrency(s.totalCost, currency)}) будет удалена навсегда.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession(s)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 3. Add Expense Dialog
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            defaultOdometer = defaultOdometer,
            topCategories = topCategories,
            currency = currency,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { category, amount, odometer, comment ->
                onAddExpense?.invoke(category, amount, odometer, comment)
                showAddExpenseDialog = false
            }
        )
    }

    // 4. Edit Expense Dialog
    if (editingExpense != null) {
        val exp = editingExpense!!
        AddEditExpenseDialog(
            initialExpense = exp,
            defaultOdometer = defaultOdometer,
            topCategories = topCategories,
            currency = currency,
            onDismiss = { editingExpense = null },
            onSave = { category, amount, odometer, comment ->
                val updated = exp.copy(
                    category = category,
                    amount = amount,
                    odometer = odometer,
                    comment = comment
                )
                onUpdateExpense?.invoke(updated)
                editingExpense = null
            }
        )
    }

    // 5. Confirm Delete Expense Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить этот расход?") },
            text = {
                Text("Расход \"${getCategoryEmoji(exp.category)} ${exp.category}\" на сумму ${formatCurrency(exp.amount, currency)} будет удален.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense?.invoke(exp)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 6. Share Session Card Dialog
    if (sessionToShare != null) {
        ShareSessionCardDialog(
            session = sessionToShare!!,
            car = activeCar,
            currency = currency,
            onDismiss = { sessionToShare = null }
        )
    }
}

@Composable
fun HistorySessionItemCard(
    session: ChargingSession,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDc = session.stationType.equals("DC", ignoreCase = true)
    val isHome = session.operatorName.contains("Дом", ignoreCase = true)
    val badgeColor = when {
        isHome -> BatteryGreen
        isDc -> Color(0xFFC084FC) // Purple
        else -> Color(0xFF38BDF8) // Light Blue AC
    }

    VoltCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_session_item_${session.id}"),
        borderColor = badgeColor.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)
                ) {
                    // Modern clean color badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isHome) "HOME" else session.stationType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = session.operatorName.ifEmpty { "Зарядная станция" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(session.startTime),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(session.totalCost, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ElectricCyan
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", session.pricePerKwh)} $currency/кВт·ч",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SoC & kWh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.startSoc.toInt()}% → ${session.endSoc.toInt()}% (+${String.format(Locale.US, "%.1f", session.kwhDeliveredByStation)} кВт·ч)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Odometer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = SoftBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.startOdometer.toInt()} км",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!session.operatorComment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.operatorComment,
                        fontSize = 11.sp,
                        color = SoftBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryExpenseItemCard(
    expense: CarExpense,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = getCategoryEmoji(expense.category)

    VoltCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("history_expense_item_${expense.id}"),
        borderColor = SoftBlue.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    // Category Emoji Avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftBlue.copy(alpha = 0.15f))
                            .border(1.dp, SoftBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = expense.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(expense.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(expense.amount, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ElectricCyan
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = SoftBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Odometer and Comment details
            if (expense.odometer != null || !expense.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!expense.comment.isNullOrBlank()) {
                        Text(
                            text = expense.comment,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (expense.odometer != null && expense.odometer > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = SoftBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${expense.odometer.toInt()} км",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoftBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
