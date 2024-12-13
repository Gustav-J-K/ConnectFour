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

import com.google.firebase.firestore.ListenerRegistration

import android.util.Log

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

import androidx.compose.runtime.Composable

import com.google.firebase.firestore.DocumentReference

import androidx.compose.material3.TextField

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text

import androidx.compose.material3.OutlinedTextField

import androidx.compose.foundation.border

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

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
            ConnectFourOff(navController = navController)
        }
        composable("online_game") {
            OnlineGameOptions(navController)
        }
        composable("join_game") {
            JoinGameScreen(navController)
        }
        composable("waiting_for_player2/{gameCode}") { backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            WaitingForPlayer2Screen(navController = navController, gameCode = gameCode)
        }
        composable("game_screen/{gameCode}") { backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            GameScreen(gameCode = gameCode, navController = navController)
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
            onClick = {
                // Skapa ett nytt spel och navigera direkt till WaitingForPlayer2Screen
                val gameCode = generateGameCode()

                // Skapa spelet i Firestore
                createGame(gameCode) { success, message ->
                    if (success) {
                        // Navigera till WaitingForPlayer2Screen om spelet skapades framgångsrikt
                        navController.navigate("waiting_for_player2/$gameCode")
                    } else {
                        // Hantera fel om spelet inte skapades korrekt
                        Log.e("CreateGame", message)
                    }
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Create a New Game")
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
fun JoinGameScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var gameCode by remember { mutableStateOf("") }
    var isJoined by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Titel
        Text(
            text = "Join an Existing Game",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )


        TextField(
            value = gameCode,
            onValueChange = {
                // Kontrollera att endast siffror skrivs in och inte fler än 4 siffror
                if (it.all { char -> char.isDigit() } && it.length <= 4) {
                    gameCode = it
                }
            },
            label = { Text("Game Code") },
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 16.dp)
        )


        Button(
            onClick = {

                joinGame(gameCode, db) { success, message ->
                    if (success) {
                        isJoined = true
                        navController.navigate("game_screen/$gameCode")
                    } else {
                        errorMessage = message
                    }
                }
            },
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Join Game")
        }

        // Visar ett meddelande när spelet är anslutet
        if (isJoined) {
            Text("Game joined successfully! Waiting for Player 2...", color = Color.Green)
        }

        // Visar ett felmeddelande om något gick fel
        if (errorMessage.isNotEmpty()) {
            Text("Error: $errorMessage", color = Color.Red)
        }
    }
}

fun joinGame(gameCode: String, db: FirebaseFirestore, onResult: (Boolean, String) -> Unit) {
    val gameRef = db.collection("games").document(gameCode)

    // Kontrollera om spelet finns
    gameRef.get().addOnSuccessListener { documentSnapshot ->
        if (documentSnapshot.exists()) {
            // Kolla om player2 redan är ansluten
            val player2 = documentSnapshot.getString("player2")
            if (player2.isNullOrEmpty()) {
                // Uppdatera spelet med player2
                gameRef.update(
                    "player2", "player2",
                    "status", "InProgress"
                )
                    .addOnSuccessListener {
                        onResult(true, "Successfully joined the game.")
                    }
                    .addOnFailureListener { e ->
                        onResult(false, "Error joining the game: ${e.message}")
                    }
            }
        } else {
            // Om spelet inte finns
            onResult(false, "Game not found.")
        }
    }.addOnFailureListener { e ->
        onResult(false, "Error fetching game data: ${e.message}")
    }
}

fun generateGameCode(): String {
    return (1000..9999).random().toString()
}

fun createGame(gameCode: String, onResult: (Boolean, String) -> Unit) {

    val db = FirebaseFirestore.getInstance()
    val gameRef = db.collection("games").document(gameCode)

    // Hämta spelet för att kontrollera om det redan finns
    gameRef.get().addOnSuccessListener { documentSnapshot ->
        if (documentSnapshot.exists()) {
            // Om spelet redan finns, ge ett felmeddelande
            onResult(false, "Game already exists.")
        } else {
            // Om spelet inte finns, skapa spelet
            val gameData = mapOf(
                "gameCode" to gameCode,
                "player1" to "player1", // Hårdkodad till "player1"
                "player2" to null, // Player2 är ännu inte ansluten
                "status" to "waiting", // Spelet väntar på player2
                "winner" to null
            )
            gameRef.set(gameData)
                .addOnSuccessListener {
                    // Om spelet skapades framgångsrikt, kalla på onResult
                    onResult(true, "Game successfully created.")
                }
                .addOnFailureListener { e ->
                    // Om det är ett fel, ge ett felmeddelande
                    onResult(false, "Error creating game: ${e.message}")
                }
        }
    }.addOnFailureListener { e ->
        // Om det är ett fel att hämta spelet, ge ett felmeddelande
        onResult(false, "Error checking game existence: ${e.message}")
    }
}


@Composable
fun WaitingForPlayer2Screen(navController: NavController, gameCode: String) {
    val context = LocalContext.current
    var isPlayer2Joined by remember { mutableStateOf(false) }

    // Lyssna på spelets status i Firestore
    val db = FirebaseFirestore.getInstance()

    // Lyssna på dokumentet för spelet och kolla om spelare 2 har anslutit
    LaunchedEffect(gameCode) {
        val gameRef = db.collection("games").document(gameCode)

        gameRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.w("WaitingForPlayer2Screen", "Listen failed.", exception)
                return@addSnapshotListener
            }

            // Kolla om spelet finns och om spelare 2 har anslutit
            if (snapshot != null && snapshot.exists()) {
                val gameData = snapshot.data
                val player2 = gameData?.get("player2")

                // Om player2 har anslutit, navigera till GameScreen
                if (player2 != null) {
                    isPlayer2Joined = true
                    // Navigera till GameScreen när spelare 2 har anslutit
                    navController.navigate("game_screen/$gameCode")
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Waiting for Player 2...",
                color = Color.Black,
                style = TextStyle(fontSize = 20.sp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Game Code: $gameCode",
                color = Color.Black,
                style = TextStyle(fontSize = 16.sp)
            )
        }
    }
}

class GameViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val gameRef = db.collection("games")

    var board: Array<IntArray> = Array(ROWS) { IntArray(COLUMNS) { 0 } }
    var currentPlayer by mutableStateOf(1)
    var winner by mutableStateOf(0)
    var isTurnLocked by mutableStateOf(false)

    fun makeMove(gameCode: String, column: Int) {
        if (winner != 0 || isTurnLocked) return

        // Find the lowest available row in the column
        for (row in ROWS - 1 downTo 0) {
            if (board[row][column] == 0) {
                board[row][column] = currentPlayer
                if (checkWinner(board, currentPlayer)) {
                    winner = currentPlayer
                }
                switchPlayer()
                updateGameState(gameCode)
                break
            }
        }
    }

    fun updateGameState(gameCode: String) {
        val gameDoc = gameRef.document(gameCode)

        // Skapa en map av spelets tillstånd som har rätt typ
        val gameState: Map<String, Any> = mapOf(
            "board" to board.map { it.toList() }.flatten(),  // Flatten board korrekt
            "currentPlayer" to currentPlayer,
            "winner" to winner,
            "isTurnLocked" to isTurnLocked
        )

        // Uppdatera spelets tillstånd i Firestore
        gameDoc.set(gameState)
    }

    private fun switchPlayer() {
        currentPlayer = if (currentPlayer == 1) 2 else 1
    }

    fun checkWinner(board: Array<IntArray>, player: Int): Boolean {
        // Kontrollera horisontella rader
        for (row in 0 until ROWS) {
            for (col in 0 until COLUMNS - 3) {
                if (board[row][col] == player && board[row][col + 1] == player && board[row][col + 2] == player && board[row][col + 3] == player) {
                    return true
                }
            }
        }

        // Kontrollera vertikala kolumner
        for (col in 0 until COLUMNS) {
            for (row in 0 until ROWS - 3) {
                if (board[row][col] == player && board[row + 1][col] == player && board[row + 2][col] == player && board[row + 3][col] == player) {
                    return true
                }
            }
        }

        // Kontrollera diagonala linjer (från vänster till höger)
        for (row in 0 until ROWS - 3) {
            for (col in 0 until COLUMNS - 3) {
                if (board[row][col] == player && board[row + 1][col + 1] == player && board[row + 2][col + 2] == player && board[row + 3][col + 3] == player) {
                    return true
                }
            }
        }

        // Kontrollera diagonala linjer (från höger till vänster)
        for (row in 3 until ROWS) {
            for (col in 0 until COLUMNS - 3) {
                if (board[row][col] == player && board[row - 1][col + 1] == player && board[row - 2][col + 2] == player && board[row - 3][col + 3] == player) {
                    return true
                }
            }
        }

        return false
    }


    fun syncGameWithFirestore(gameCode: String) {
        viewModelScope.launch {
            val gameDoc = gameRef.document(gameCode)
            gameDoc.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Hantera eventuella fel här
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val gameData = snapshot.data
                    val onlineBoard = gameData?.get("board") as? List<Long>
                    if (onlineBoard != null) {
                        // Om brädet är i en annan form, här konverterar vi det till IntArray
                        board = Array(ROWS) { row ->
                            IntArray(COLUMNS) { col ->
                                onlineBoard[row * COLUMNS + col].toInt()
                            }
                        }
                        currentPlayer = (gameData["currentPlayer"] as? Long)?.toInt() ?: 1
                        winner = (gameData["winner"] as? Long)?.toInt() ?: 0
                        isTurnLocked = (gameData["isTurnLocked"] as? Boolean) ?: false
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(gameCode: String, navController: NavController) {
    val viewModel: GameViewModel = viewModel()

    // Sync game data with Firestore
    viewModel.syncGameWithFirestore(gameCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (viewModel.winner == 0) "Player ${viewModel.currentPlayer}'s turn" else "Player ${viewModel.winner} wins!",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vise brädet och gör drag, lås eller lås upp beroende på tur
        for (row in 0 until ROWS) {
            Row {
                for (col in 0 until COLUMNS) {
                    Cell(value = viewModel.board[row][col]) {
                        if (viewModel.winner == 0 && viewModel.board[row][col] == 0 && !viewModel.isTurnLocked) {
                            viewModel.makeMove(gameCode, col)
                        }
                    }
                }
            }
        }

        if (viewModel.winner != 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Navigera tillbaka till GameModeSelectionScreen istället för att återställa spelet
                navController.navigate("game_mode_selection")
            }) {
                Text("Back to Game Mode Selection")
            }
        }
    }
}

@Composable
fun ConnectFourOff(navController: NavController) {
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
        Spacer(modifier = Modifier.height(16.dp))

        // Knappen för att gå tillbaka till GameModeSelection
        Button(
            onClick = { navController.navigate("game_mode_selection") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Back to Game Mode Selection")
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
