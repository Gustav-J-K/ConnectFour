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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent {
            GameModeSelectionScreen()
        }
    }
}

const val ROWS = 6
const val COLUMNS = 7

@Composable
fun GameModeSelectionScreen() {
    // State för att hålla koll på vald spelläge
    var selectedMode by remember { mutableStateOf<String?>(null) }

    if (selectedMode == null) {
        // Visa val för spelläge
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
                    }
                    .padding(16.dp)
            )
        }
    } else if (selectedMode == "offline") {
        // Visa offline-spelskärmen
        ConnectFourGame()
    } else if (selectedMode == "online") {
        // Placeholder för online-spelskärm
        Text(
            text = "Online mode is under construction!",
            fontSize = 20.sp,
            color = Color.Red,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .wrapContentSize(Alignment.Center)
        )
    }
}


@Composable
fun ConnectFourGame() {
    // Representera spelbrädet: 0 = tom, 1 = spelare 1, 2 = spelare 2
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

        // Spelbräde
        for (row in 0 until ROWS) {
            Row(modifier = Modifier.padding(4.dp)) {
                for (col in 0 until COLUMNS) {
                    Cell(
                        value = board[row][col],
                        onClick = {
                            if (winner == 0) {
                                makeMove(board, col, currentPlayer)?.let {
                                    if (checkWin(board, currentPlayer)) {
                                        winner = currentPlayer
                                    } else if (board.all { it.all { cell -> cell != 0 } }) {
                                        winner = -1 // Oavgjort
                                    } else {
                                        currentPlayer = 3 - currentPlayer // Växla spelare
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

fun makeMove(board: MutableList<IntArray>, column: Int, player: Int): Int? {
    for (row in ROWS - 1 downTo 0) {
        if (board[row][column] == 0) {
            board[row][column] = player
            return row
        }
    }
    return null
}

fun checkWin(board: List<IntArray>, player: Int): Boolean {
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

