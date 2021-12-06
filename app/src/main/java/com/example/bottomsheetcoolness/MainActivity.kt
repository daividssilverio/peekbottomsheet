package com.example.bottomsheetcoolness

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bottomsheetcoolness.ui.theme.BottomSheetCoolnessTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            BottomSheetCoolnessTheme {
                ProvideWindowInsets {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            CustomApp(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegularApp(modifier: Modifier) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    ModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator
    ) {
        Scaffold { paddings ->
            NavHost(
                modifier = Modifier.padding(paddings),
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    MainPage(navController)
                }
                bottomSheet("niceSheet") {
                    Box {
                        NiceSheet(onClick = { i ->
                            navController.navigate(if (i % 2 == 0) "nested1" else "nested2")
                        })
                    }
                }
                bottomSheet("nested1") {
                    NiceNestedSheet()
                }
                bottomSheet("nested2") {
                    NiceNestedSheet2()
                }
            }
        }
    }
}

@Composable
fun CustomApp(modifier: Modifier) {
    val bottomSheetNavigator = rememberTogglBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val currentRoute by navController.currentBackStackEntryAsState()
    TogglModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
        peekHeight = remember(currentRoute) { currentRoute.toPeekHeight() }
    ) {
        Scaffold { paddings ->
            NavHost(
                modifier = Modifier.padding(paddings),
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    MainPage(navController)
                }
                togglBottomSheet("niceSheet") {
                    Box {
                        NiceSheet(onClick = { i ->
                            navController.navigate(if (i % 2 == 0) "nested1" else "nested2")
                        })
                    }
                }
                togglBottomSheet("nested1") {
                    NiceNestedSheet()
                }
                togglBottomSheet("nested2") {
                    NiceNestedSheet2()
                }
            }
        }
    }
}

private fun NavBackStackEntry?.toPeekHeight(): Dp {
    val displayName = this?.destination?.route
    Log.d("xxaa", "$displayName")
    return when(displayName) {
        "niceSheet" -> 150.dp
        "nested1" -> 100.dp
        "nested2" -> 200.dp
        else -> 0.dp
    }
}

@Composable
fun MainPage(navController: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Cyan)
    ) {
        Text(text = "hello")
        Button(onClick = { navController.navigate("niceSheet") }) {
            Text(text = "Open Sheet")
        }
    }
}

@Composable
fun NiceSheet(onClick: (Int) -> Unit) {
    val focusRequester = remember {
        FocusRequester()
    }
    Column {
        val (text, setText) = remember {
            mutableStateOf("first")
        }
        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
                .systemBarsPadding()
        ) {
            TextField(
                value = text,
                onValueChange = setText,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Magenta)
                .background(Color.Yellow)
                .height(400.dp)
        ) {
            items(7) { i ->
                Box(modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .border(1.dp, Color.Cyan)
                    .clickable { onClick(i) }
                ) {
                    Text(text = "$i")
                }
            }
        }
    }
}

@Composable
fun NiceNestedSheet() {
    val focusRequester = remember {
        FocusRequester()
    }
    Column {
        val (text, setText) = remember {
            mutableStateOf("so nested")
        }
        Box(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
                .systemBarsPadding()
        ) {
            TextField(
                value = text,
                onValueChange = setText,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Magenta)
                .background(Color.Yellow)
                .height(400.dp)
        ) {
            items(4) { i ->
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .border(1.dp, Color.Cyan)
                ) {
                    Text(text = "$i")
                }
            }
            item {

            }
        }
    }
}

@Composable
fun NiceNestedSheet2() {
    val focusRequester = remember {
        FocusRequester()
    }
    Column {
        val (text, setText) = remember {
            mutableStateOf("so nested 2")
        }
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
                .systemBarsPadding()
        ) {
            TextField(
                value = text,
                onValueChange = setText,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Magenta)
                .background(Color.Yellow)
                .height(400.dp)
        ) {
            items(4) { i ->
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .border(1.dp, Color.Cyan)
                ) {
                    Text(text = "$i")
                }
            }
        }
    }
}