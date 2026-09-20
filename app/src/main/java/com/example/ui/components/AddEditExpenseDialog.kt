package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CarExpense
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import java.util.Locale

val ALL_EXPENSE_CATEGORIES = listOf(
    "Мойка" to "🧼",
    "ТО" to "🔧",
    "Страховка" to "🛡️",
    "Шиномонтаж" to "🛞",
    "Парковка" to "🅿️",
    "Омывайка и химия" to "🧪",
    "Ремонт" to "🛠️",
    "Тюнинг и аксессуары" to "⚙️",
    "Налоги и сборы" to "🏛️",
    "Штрафы" to "📄",
    "Другое" to "💼"
)

fun getCategoryEmoji(category: String): String {
    return ALL_EXPENSE_CATEGORIES.firstOrNull { it.first.equals(category.trim(), ignoreCase = true) }?.second ?: "💳"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    initialExpense: CarExpense? = null,
    defaultOdometer: Double? = null,
    topCategories: List<String> = listOf("Мойка", "ТО", "Страховка", "Шиномонтаж", "Парковка"),
    currency: String = "BYN",
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, odometer: Double?, comment: String?) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(initialExpense?.category ?: topCategories.firstOrNull() ?: "Мойка")
    }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var amountText by remember {
        mutableStateOf(if (initialExpense != null && initialExpense.amount > 0) String.format(Locale.US, "%.2f", initialExpense.amount) else "")
    }
    var odometerText by remember {
        val odo = initialExpense?.odometer ?: defaultOdometer
        mutableStateOf(if (odo != null && odo > 0) String.format(Locale.US, "%.0f", odo) else "")
    }
    var commentText by remember {
        mutableStateOf(initialExpense?.comment ?: "")
    }

    val parsedAmount = amountText.replace(',', '.').toDoubleOrNull()
    val isFormValid = parsedAmount != null && parsedAmount > 0.0 && selectedCategory.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialExpense != null) "Редактировать расход" else "Добавить расход",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Категория расхода:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick chips: Top 5 user categories
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayChips = (topCategories.take(5) + listOf(selectedCategory)).distinct()
                    displayChips.forEach { cat ->
                        val emoji = getCategoryEmoji(cat)
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text("$emoji $cat", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }

                // Dropdown to pick any category
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCategoryDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "${getCategoryEmoji(selectedCategory)} $selectedCategory (выбрать другую)",
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        ALL_EXPENSE_CATEGORIES.forEach { (catName, emoji) ->
                            DropdownMenuItem(
                                text = { Text("$emoji $catName", fontSize = 14.sp) },
                                onClick = {
                                    selectedCategory = catName
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма ($currency) *") },
                    placeholder = { Text("напр. 35.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // Odometer input
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("Пробег на момент расхода (км)") },
                    placeholder = { Text("необязательно") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_odometer_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // Comment input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Комментарий / Детали") },
                    placeholder = { Text("напр. Комплексная мойка с воском") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_comment_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid && parsedAmount != null) {
                        val odo = odometerText.replace(',', '.').toDoubleOrNull()
                        onSave(
                            selectedCategory,
                            parsedAmount,
                            odo,
                            commentText.trim().ifBlank { null }
                        )
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isFormValid) Brush.horizontalGradient(listOf(SoftBlue, ElectricCyan))
                        else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                    )
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Отмена")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
