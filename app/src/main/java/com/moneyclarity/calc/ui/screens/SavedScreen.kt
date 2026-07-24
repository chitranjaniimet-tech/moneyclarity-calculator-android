package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.SavedCalc
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.engine.Finance
import com.moneyclarity.calc.engine.monthsToTenure
import com.moneyclarity.calc.engine.percent
import com.moneyclarity.calc.engine.rupees
import com.moneyclarity.calc.ui.components.*

@Composable
fun SavedScreen(state: CalcState, onLoaded: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<SavedCalc>>(emptyList()) }
    LaunchedEffect(Unit) { items = Store.load(context) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (items.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Nothing kept yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Work out an instalment and tap Save. Whatever you keep stays on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val emi = Finance.emi(item.amount, item.rate, item.months)
                    SectionCard {
                        Eyebrow(item.name)
                        Spacer(Modifier.height(8.dp))
                        StatRow("Instalment", rupees(emi), emphasis = true)
                        HairlineDivider()
                        StatRow("Rate and tenure", "${percent(item.rate)} for ${monthsToTenure(item.months)}")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    state.set(item.amount, item.rate, item.months)
                                    onLoaded()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Open") }
                            OutlinedButton(
                                onClick = { items = Store.remove(context, item.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
