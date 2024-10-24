package com.example.shoppinglist

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.example.shoppinglist.ui.theme.ShoppingListTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        enableEdgeToEdge()
        val context : Context = this;
        setContent {
            ShoppingListTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShoppingList(Modifier.padding(innerPadding),context);
                }
            }
        }
    }
}

data class Product(
    val id : String,
    val name : String,
    val quantity : Int,
    var isPurchased : Boolean = false
)

@Composable
fun ShoppingList(modifier: Modifier = Modifier,context: Context){

    val sharedPreferences = context.getSharedPreferences("list_prefs",Context.MODE_PRIVATE)
    val gson = remember { Gson() }

    var showDialog by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }
    var productList by rememberSaveable { mutableStateOf(loadProductList(sharedPreferences,gson)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver{_,event ->
            if (event == Lifecycle.Event.ON_PAUSE){
                Log.d("LifecycleOwnerCheck","Current LifecycleOwner is: ${lifecycleOwner::class.simpleName}")
                saveProductList(sharedPreferences,gson, productList)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Column (modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Button(
            onClick = {showDialog = true}

        ) { Text("add product")}
        LazyColumn {
            items(productList){ product ->
                ProductRow(product,
                    onPurchaseClick = {
                        productList = productList.map {
                            if (it.id == product.id){
                                it.copy(isPurchased = !it.isPurchased)
                            }else{
                                it
                            }
                        }
                    },
                    onDeleteClick = {
                        productList = productList.filter { it.id !=product.id }
                    }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )
            }
        }
    }
    if (showDialog){
        AlertDialog(
            onDismissRequest = {showDialog = false},
            confirmButton = {
                Button(
                    onClick = {
                        var product = Product(
                            id = UUID.randomUUID().toString(),
                            name = productName,
                            quantity = productQuantity.toIntOrNull() ?: 1
                        )
                        productList = productList + product
                        showDialog = false
                        productName = ""
                        productQuantity = ""
                        saveProductList(sharedPreferences,gson,productList)
                    }
                ) {
                    Text("add")
                }
            },
            title = { Text("Add product to list") },
            text = {
                Column {
                    Text("Nazwa")
                    TextField(value = productName, onValueChange = {productName = it})
                    Text("Ilość")
                    TextField(value = productQuantity, onValueChange = {productQuantity= it} )
                }
            }
        )
    }
}
private fun loadProductList(sharedPreferences: android.content.SharedPreferences, gson: Gson): List<Product> {
    val jsonString = sharedPreferences.getString("product_list",null)
    return if(jsonString != null){
        val type = object : TypeToken<List<Product>>(){}.type
        gson.fromJson(jsonString,type)}
    else{
        emptyList()
    }
}
private fun saveProductList(sharedPreferences: SharedPreferences,gson:Gson,productList:List<Product>){
    val editor = sharedPreferences.edit()
    val jsonString =gson.toJson(productList)
    editor.putString("product_list",jsonString)
    editor.apply()
}



@Composable
fun ProductRow(product: Product,
               onPurchaseClick: () -> Unit,
               onDeleteClick: () -> Unit)
{

    val rowMod = Modifier.fillMaxWidth()
        .height(80.dp)
        .padding(10.dp)
        .clickable(onClick = onPurchaseClick)

    val textDecor : TextDecoration;
    val color : Color;
    if (product.isPurchased){
        textDecor = TextDecoration.LineThrough
        color = Color.Gray
    }
    else{
        textDecor = TextDecoration.None
        color = Color.Black
    }
    Row (
        modifier = rowMod,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Absolute.SpaceBetween
    ){
            Text("${product.name} : ${product.quantity}",
                textDecoration = textDecor,
                color = color)
            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = color)
            }
    }
}