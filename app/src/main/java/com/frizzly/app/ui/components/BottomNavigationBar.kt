package com.frizzly.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

sealed class BottomNavItem(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Home : BottomNavItem("home", { Icon(Icons.Filled.Home, contentDescription = "Home") }, "Home")
    object Categories : BottomNavItem("categories", { Icon(Icons.Filled.Category, contentDescription = "Categories") }, "Categories")
    object Orders : BottomNavItem("orders", { Icon(Icons.Filled.ShoppingCart, contentDescription = "Orders") }, "Orders")
    object Profile : BottomNavItem("profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") }, "Profile")
}

@Composable
fun FrizzlyBottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Categories,
        BottomNavItem.Orders,
        BottomNavItem.Profile
    )

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                icon = item.icon,
                label = { Text(item.label) },
                selected = selectedItem == item,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFrizzlyBottomNavigationBar() {
    FrizzlyBottomNavigationBar(
        selectedItem = BottomNavItem.Home,
        onItemSelected = {}
    )
}
