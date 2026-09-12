package com.abunashmi.azkar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class Dhikr(
    val text: String,
    val count: Int,
    val category: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AzkarApp() }
    }
}

private val Bg = Color(0xFFF7F3E9)
private val Green = Color(0xFF245C4A)
private val Gold = Color(0xFFC79A3B)

@Composable
fun AzkarApp() {
    val prefs = androidx.compose.ui.platform.LocalContext.current
        .getSharedPreferences("azkar", 0)

    var items by remember {
        mutableStateOf(loadItems(prefs))
    }
    var admin by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            secondary = Gold,
            background = Bg,
            surface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            if (admin) {
                AdminScreen(
                    items = items,
                    onBack = { admin = false },
                    onAdd = {
                        editingIndex = null
                        showEditor = true
                    },
                    onEdit = {
                        editingIndex = it
                        showEditor = true
                    },
                    onDelete = {
                        items = items.toMutableList().also { list -> list.removeAt(it) }
                        saveItems(prefs, items)
                    }
                )
            } else {
                HomeScreen(
                    items = items,
                    category = selectedCategory,
                    onCategory = { selectedCategory = it },
                    onAdmin = { pinDialog = true }
                )
            }

            if (pinDialog) {
                PinDialog(
                    onDismiss = { pinDialog = false },
                    onSuccess = {
                        pinDialog = false
                        admin = true
                    }
                )
            }

            if (showEditor) {
                EditorDialog(
                    initial = editingIndex?.let { items[it] },
                    onDismiss = { showEditor = false },
                    onSave = { text, count, category ->
                        val updated = items.toMutableList()
                        val d = Dhikr(text, count, category)
                        if (editingIndex == null) updated.add(d)
                        else updated[editingIndex!!] = d
                        items = updated
                        saveItems(prefs, items)
                        showEditor = false
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    items: List<Dhikr>,
    category: String,
    onCategory: (String) -> Unit,
    onAdmin: () -> Unit
) {
    val categories = listOf("الكل", "أذكار الصباح", "أذكار المساء", "بعد الصلاة", "النوم")
    val shown = if (category == "الكل") items else items.filter { it.category == category }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("أذكار أبونشمي", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Green)
                Text("اليومية", fontSize = 18.sp, color = Gold)
            }
            TextButton(onClick = onAdmin) { Text("لوحة التحكم") }
        }

        Spacer(Modifier.height(14.dp))
        LazyColumn(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { onCategory(c) },
                            label = { Text(c, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            itemsIndexed(shown) { _, d ->
                DhikrCard(d)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun DhikrCard(d: Dhikr) {
    var remaining by remember(d.text, d.count) { mutableIntStateOf(d.count) }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(d.category, color = Gold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                d.text,
                fontSize = 21.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF202020)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (remaining > 0) remaining-- },
                enabled = remaining > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text(if (remaining == 0) "تم ✓" else "اضغط للتكرار: $remaining")
            }
        }
    }
}

@Composable
fun AdminScreen(
    items: List<Dhikr>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("لوحة التحكم", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Green)
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) { Text("＋ إضافة ذكر") }

        Spacer(Modifier.height(12.dp))
        LazyColumn {
            itemsIndexed(items) { index, d ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.category, color = Gold)
                        Text(d.text, fontSize = 17.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Text("التكرار: ${d.count}")
                        Row {
                            TextButton(onClick = { onEdit(index) }) { Text("تعديل") }
                            TextButton(onClick = { onDelete(index) }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دخول المدير") },
        text = {
            Column {
                Text("أدخل رمز لوحة التحكم")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = false },
                    singleLine = true
                )
                if (error) Text("الرمز غير صحيح", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin == "1234") onSuccess() else error = true
            }) { Text("دخول") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun EditorDialog(
    initial: Dhikr?,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var count by remember { mutableStateOf((initial?.count ?: 3).toString()) }
    var category by remember { mutableStateOf(initial?.category ?: "أذكار الصباح") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة ذكر" else "تعديل الذكر") },
        text = {
            Column {
                OutlinedTextField(text, { text = it }, label = { Text("نص الذكر") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(count, { count = it.filter(Char::isDigit) }, label = { Text("عدد التكرارات") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(category, { category = it }, label = { Text("القسم") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = count.toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (text.isNotBlank()) onSave(text.trim(), n, category.ifBlank { "عام" })
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun loadItems(prefs: android.content.SharedPreferences): List<Dhikr> {
    val raw = prefs.getString("items", null)
    if (raw == null) return listOf(
        Dhikr("سُبْحَانَ اللهِ وَبِحَمْدِهِ", 100, "أذكار الصباح"),
        Dhikr("أستغفرُ اللهَ وأتوبُ إليه", 100, "أذكار المساء"),
        Dhikr("اللهم أعني على ذكرك وشكرك وحسن عبادتك", 1, "بعد الصلاة"),
        Dhikr("باسمك اللهم أموت وأحيا", 1, "النوم")
    )
    return try {
        val a = JSONArray(raw)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Dhikr(o.getString("text"), o.getInt("count"), o.getString("category"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveItems(prefs: android.content.SharedPreferences, items: List<Dhikr>) {
    val a = JSONArray()
    items.forEach {
        a.put(JSONObject().apply {
            put("text", it.text)
            put("count", it.count)
            put("category", it.category)
        })
    }
    prefs.edit().putString("items", a.toString()).apply()
}
