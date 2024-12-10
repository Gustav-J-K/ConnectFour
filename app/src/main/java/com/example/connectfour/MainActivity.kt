package com.example.connectfour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
//import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.min

import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import kotlin.random.Random
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


val db = Firebase.firestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent {
            AppNavigation()
        }
    }
}

const val ROWS = 6
const val COLUMNS = 7

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "game_mode_selection") {
        composable("game_mode_selection") {
            GameModeSelectionScreen(navController)
        }
        composable("offline_game") {
            ConnectFourOff()
        }
        composable("online_game") {
            OnlineGameOptions(navController)
        }
        composable("join_game") {
            JoinGameScreen()
        }
        composable("create_game") {
            CreateGameScreen()
        }
    }
}

@Composable
fun GameModeSelectionScreen(navController: NavController) {
    var selectedMode by remember { mutableStateOf<String?>(null) }

    if (selectedMode == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Game Mode",
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Play Offline",
                fontSize = 20.sp,
                color = Color.Blue,
                modifier = Modifier
                    .clickable {
                        selectedMode = "offline"
                        navController.navigate("offline_game")
                    }
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Play Online",
                fontSize = 20.sp,
                color = Color.Blue,
                modifier = Modifier
                    .clickable {
                        selectedMode = "online"
                        navController.navigate("online_game")
                    }
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun OnlineGameOptions(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose an Option",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("create_game") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Create Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("join_game") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Join Game")
        }
    }
}

@Composable
fun JoinGameScreen() {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter 4-digit code", style = MaterialTheme.typography.bodyLarge)

        BasicTextField(
            value = code,
            onValueChange = { input ->
                code = input.trim()
                if (errorMessage.isNotEmpty()) {
                    errorMessage = ""
                }
            },
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    Modifier
                        .padding(16.dp)
                        .background(Color.Gray)
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    innerTextField()
                }
            }
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                if (code.length != 4 || code.any { !it.isDigit() }) {
                    errorMessage = "You must enter exactly 4 digits!"
                } else {
                    // Simulate joining the game
                    println("Joining game with code: $code")
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Confirm")
        }
    }
}

fun generateGameCode(): String {
    return (1000..9999).random().toString()
}


@Composable
fun CreateGameScreen() {
    val gameCode = remember { generateGameCode() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Waiting for players...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Code: $gameCode",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}



@Composable
fun ConnectFourOff() {
    val board = remember { mutableStateListOf(*Array(ROWS) { IntArray(COLUMNS) { 0 } }) }
    var currentPlayer by remember { mutableStateOf(1) }
    var winner by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                winner == 0 -> "Player $currentPlayer's turn"
                winner > 0 -> "Player $winner wins!"
                else -> "It's a draw!"
            },
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        for (row in 0 until ROWS) {
            Row(modifier = Modifier.padding(4.dp)) {
                for (col in 0 until COLUMNS) {
                    Cell(
                        value = board[row][col],
                        onClick = {
                            if (winner == 0) {
                                makeMoveOffline(board, col, currentPlayer)?.let {
                                    if (checkWinOffline(board, currentPlayer)) {
                                        winner = currentPlayer
                                    } else if (board.all { it.all { cell -> cell != 0 } }) {
                                        winner = -1
                                    } else {
                                        currentPlayer = 3 - currentPlayer
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        if (winner != 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Restart",
                fontSize = 20.sp,
                color = Color.Blue,
                modifier = Modifier
                    .clickable {
                        for (row in 0 until ROWS) {
                            for (col in 0 until COLUMNS) {
                                board[row][col] = 0
                            }
                        }
                        winner = 0
                        currentPlayer = 1
                    }
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun Cell(value: Int, onClick: () -> Unit) {
    val color = when (value) {
        1 -> Color.Red
        2 -> Color.Yellow
        else -> Color.White
    }

    Canvas(
        modifier = Modifier
            .size(50.dp)
            .padding(4.dp)
            .clickable { if (value == 0) onClick() }
    ) {
        drawCircle(color = color)
    }
}

fun makeMoveOffline(board: MutableList<IntArray>, column: Int, player: Int): Int? {
    for (row in ROWS - 1 downTo 0) {
        if (board[row][column] == 0) {
            board[row][column] = player
            return row
        }
    }
    return null
}

fun checkWinOffline(board: List<IntArray>, player: Int): Boolean {
    // Horisontella
    for (row in 0 until ROWS) {
        for (col in 0 until COLUMNS - 3) {
            if ((0..3).all { board[row][col + it] == player }) return true
        }
    }

    // Vertikala
    for (col in 0 until COLUMNS) {
        for (row in 0 until ROWS - 3) {
            if ((0..3).all { board[row + it][col] == player }) return true
        }
    }

    // Diagonala(från vänster till höger)
    for (row in 0 until ROWS - 3) {
        for (col in 0 until COLUMNS - 3) {
            if ((0..3).all { board[row + it][col + it] == player }) return true
        }
    }

    // Diagonala(från höger till vänster)
    for (row in 0 until ROWS - 3) {
        for (col in 3 until COLUMNS) {
            if ((0..3).all { board[row + it][col - it] == player }) return true
        }
    }

    return false
}
