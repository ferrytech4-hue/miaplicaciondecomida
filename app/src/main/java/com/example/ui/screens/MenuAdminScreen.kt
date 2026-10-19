package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.components.AlcoholWarningBadge
import com.example.ui.components.ChileanPriceText
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MenuAdminScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val allCompanies by viewModel.allCompanies.collectAsState()
    val company = selectedCompany ?: allCompanies.firstOrNull()

    if (company == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay negocio seleccionado.")
        }
        return
    }

    val products by viewModel.repository.getProductsByCompany(company.id).collectAsState(initial = emptyList())
    val categories by viewModel.repository.getCategoriesByCompany(company.id).collectAsState(initial = emptyList())

    var showNewProductDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    var newProdName by remember { mutableStateOf("") }
    var newProdDesc by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdIsAlcohol by remember { mutableStateOf(false) }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }

    var newCatName by remember { mutableStateOf("") }

    // Dialog: Nuevo Producto
    if (showNewProductDialog) {
        val catId = selectedCatId ?: categories.firstOrNull()?.id ?: 1L
        AlertDialog(
            onDismissRequest = { showNewProductDialog = false },
            title = { Text("Agregar Nuevo Producto al Menú", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Nombre del plato/bebida *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProdDesc,
                        onValueChange = { newProdDesc = it },
                        label = { Text("Descripción e ingredientes") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProdPrice,
                        onValueChange = { newProdPrice = it },
                        label = { Text("Precio en CLP (ej: 4900) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newProdIsAlcohol,
                            onCheckedChange = { newProdIsAlcohol = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contiene alcohol (Ley 19.925 +18)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newProdPrice.toIntOrNull() ?: 0
                        if (newProdName.isNotBlank() && price > 0) {
                            viewModel.createProduct(
                                ProductEntity(
                                    companyId = company.id,
                                    categoryId = catId,
                                    name = newProdName.trim(),
                                    description = newProdDesc.trim(),
                                    basePrice = price,
                                    imageUrl = "https://images.unsplash.com/photo-1550547660-d9450f859349?w=500",
                                    isAvailable = true,
                                    isAlcohol = newProdIsAlcohol
                                )
                            )
                            newProdName = ""
                            newProdDesc = ""
                            newProdPrice = ""
                            newProdIsAlcohol = false
                            showNewProductDialog = false
                        }
                    }
                ) {
                    Text("Crear Producto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProductDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Nueva Categoría
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Nueva Categoría de Menú", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Nombre (ej: Sándwiches, Tragos, Postres)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.createCategory(
                                CategoryEntity(
                                    companyId = company.id,
                                    name = newCatName.trim(),
                                    sortOrder = categories.size + 1
                                )
                            )
                            newCatName = ""
                            showNewCategoryDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("menu_admin_screen")
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADMINISTRACIÓN DE MENÚ",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "${company.commercialName} • Precios y Disponibilidad",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Action Buttons Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showNewProductDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Producto", fontSize = 12.sp)
                }
                Button(
                    onClick = { showNewCategoryDialog = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Categoría", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Product Items List
        item {
            Text(
                text = "Productos en Menú (${products.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(products) { product ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = product.description,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        if (product.isAlcohol) {
                            AlcoholWarningBadge(compact = true)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ChileanPriceText(amount = product.basePrice, fontSize = 13)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (product.isAvailable) "Disponible" else "Agotado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Switch(
                            checked = product.isAvailable,
                            onCheckedChange = { viewModel.toggleProductAvailability(product) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32)
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
