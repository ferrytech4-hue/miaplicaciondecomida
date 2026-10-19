package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ModifierGroupEntity
import com.example.data.model.ModifierOptionEntity
import com.example.data.model.ProductEntity
import com.example.ui.components.AlcoholWarningBadge
import com.example.ui.components.ChileanPriceText
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailModal(
    product: ProductEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    var modifierGroups by remember { mutableStateOf<List<ModifierGroupEntity>>(emptyList()) }
    var modifierOptionsMap by remember { mutableStateOf<Map<Long, List<ModifierOptionEntity>>>(emptyMap()) }
    val selectedOptions = remember { mutableStateListOf<ModifierOptionEntity>() }

    LaunchedEffect(product.id) {
        val (groups, optionsMap) = viewModel.repository.getProductModifiers(product.id)
        modifierGroups = groups
        modifierOptionsMap = optionsMap

        // Auto-select required single-choice modifiers
        groups.forEach { group ->
            if (group.isRequired && group.maxSelection == 1) {
                val opts = optionsMap[group.id]
                if (!opts.isNullOrEmpty()) {
                    selectedOptions.add(opts.first())
                }
            }
        }
    }

    val unitPrice = product.basePrice + selectedOptions.sumOf { it.priceDelta }
    val totalPrice = unitPrice * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("product_detail_modal")
        ) {
            // Header Image & Close
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Base Price
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ChileanPriceText(amount = product.basePrice, fontSize = 20)
            }

            if (product.isAlcohol) {
                Spacer(modifier = Modifier.height(8.dp))
                AlcoholWarningBadge()
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Modifiers Section
            if (modifierGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Personaliza tu pedido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                modifierGroups.forEach { group ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = group.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (group.isRequired) "Obligatorio" else "Opcional",
                                    fontSize = 11.sp,
                                    color = if (group.isRequired) MaterialTheme.colorScheme.primary else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val options = modifierOptionsMap[group.id] ?: emptyList()
                            options.forEach { opt ->
                                val isSingle = group.maxSelection == 1
                                val isSelected = selectedOptions.any { it.id == opt.id }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSingle) {
                                                // Remove existing selection from same group
                                                selectedOptions.removeAll { existing ->
                                                    options.any { it.id == existing.id }
                                                }
                                                selectedOptions.add(opt)
                                            } else {
                                                if (isSelected) {
                                                    selectedOptions.removeAll { it.id == opt.id }
                                                } else {
                                                    val groupSelections = selectedOptions.count { sel ->
                                                        options.any { it.id == sel.id }
                                                    }
                                                    if (groupSelections < group.maxSelection) {
                                                        selectedOptions.add(opt)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    if (isSingle) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null
                                        )
                                    } else {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = opt.name,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (opt.priceDelta > 0) {
                                        ChileanPriceText(
                                            amount = opt.priceDelta,
                                            fontSize = 12,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Instructions
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Instrucciones de preparación (ej: sin cebolla, palta aparte)") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Quantity and Add to Cart
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Stepper
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        viewModel.addToCart(
                            product = product,
                            quantity = quantity,
                            selectedOptions = selectedOptions.toList(),
                            notes = notes.ifBlank { null }
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_to_cart_confirm_button")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar • ${com.example.util.ChileanUtils.formatClp(totalPrice)}", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
