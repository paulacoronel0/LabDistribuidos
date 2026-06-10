const http = require('http');
const WebSocketServer = require('websocket').server;

// 1. Crear el servidor HTTP base
const server = http.createServer(function(request, response) {
    console.log((new Date()) + ' Petición HTTP recibida para ' + request.url);
    response.writeHead(404);
    response.end();
});

server.listen(8080, function() {
    console.log((new Date()) + ' Servidor escuchando en el puerto 8080');
});

// 2. Inicializar el Servidor WebSocket
const wsServer = new WebSocketServer({
    httpServer: server,
    autoAcceptConnections: false // Recomendado por la cátedra para validar orígenes
});

function originIsAllowed(origin) {
    return true; // Permitir cualquier origen para desarrollo local
}

// ── LÓGICA DEL JUEGO (ESTADO MUTABLE CENTRALIZADO) ────────────────

let waitingPlayer = null; // Jugador esperando en el lobby
let rooms = [];           // Lista de salas activas

// Combinaciones ganadoras tradicionales del Tateti
const WINNING_COMBOS = [
    [0, 1, 2], [3, 4, 5], [6, 7, 8], // Horizontales
    [0, 3, 6], [1, 4, 7], [2, 5, 8], // Verticales
    [0, 4, 8], [2, 4, 6]             // Diagonales
];

function checkWinner(board) {
    for (let combo of WINNING_COMBOS) {
        const [a, b, c] = combo;
        if (board[a] && board[a] === board[b] && board[a] === board[c]) {
            return { winner: board[a], line: combo };
        }
    }
    if (board.every(cell => cell !== null)) {
        return { winner: null, tie: true };
    }
    return null;
}

// ── MANEJO DE CONEXIONES ──────────────────────────────────────────

wsServer.on('request', function(request) {
    if (!originIsAllowed(request.origin)) {
        request.reject();
        console.log((new Date()) + ' Conexión del origen ' + request.origin + ' rechazada.');
        return;
    }

    // Aceptar la conexión usando el protocolo que definió tu frontend
    const connection = request.accept(null, request.origin); // Como no tenemos protocolos específicos, pasamos null
    console.log((new Date()) + ' Nuevo jugador conectado.');

    // Variables asociadas a esta conexión en particular
    let currentRoom = null;
    let mySymbol = null;

    // Asegúrate de que las variables globales en la parte superior del archivo estén así:
    let waitingPlayer = null; 
    let rooms = []; // Para soportar múltiples salas si fuera necesario

    // ... dentro de wsServer.on('request', function(request) { ...
    // var connection = request.accept('echo-protocol', request.origin);

    connection.on('message', function(message) {
        if (message.type !== 'utf8') return;
        
        let msg;
        try {
            msg = JSON.parse(message.utf8Data);
        } catch (e) {
            return;
        }

        switch (msg.type) {
            case 'join_room':
                // Evitar que un jugador se empareje consigo mismo al recargar
                if (waitingPlayer && waitingPlayer.connection === connection) {
                    return;
                }

                if (waitingPlayer && waitingPlayer.connection.connected) {
                    // Ya había alguien esperando, armamos la sala
                    const p1 = waitingPlayer;
                    const p2 = { connection: connection, symbol: 'O' };
                    p1.symbol = 'X';

                    const newRoom = {
                        players: [p1, p2],
                        board: Array(9).fill(null),
                        turn: 'X'
                    };

                    rooms.push(newRoom);

                    // ── CRUCIAL: Guardamos la referencia de la sala EN AMBOS objetos de conexión ──
                    p1.connection.roomRef = newRoom;
                    p2.connection.roomRef = newRoom;
                    
                    // También les asignamos su símbolo por si lo requiere la lógica
                    p1.connection.mySymbol = 'X';
                    p2.connection.mySymbol = 'O';

                    waitingPlayer = null; // Vaciamos el lobby

                    // Notificamos a ambos clientes que la partida inició
                    p1.connection.sendUTF(JSON.stringify({ type: 'game_start', board: newRoom.board, turn: newRoom.turn, symbol: 'X' }));
                    p2.connection.sendUTF(JSON.stringify({ type: 'game_start', board: newRoom.board, turn: newRoom.turn, symbol: 'O' }));

                    console.log(`Sala iniciada con DOS jugadores reales: (X) vs (O)`);
                } else {
                    // Primer jugador en llegar, se queda esperando en el lobby
                    waitingPlayer = { connection: connection, symbol: 'X' };
                    connection.mySymbol = 'X';
                    connection.sendUTF(JSON.stringify({ type: 'waiting', symbol: 'X' }));
                    console.log('Jugador X esperando oponente legítimo...');
                }
                break;

            case 'move':
                // ── SOLUCIÓN AL ERROR: Buscamos la sala desde la conexión que emitió el mensaje ──
                const playerRoom = connection.roomRef; 
                const playerSymbol = connection.mySymbol;

                if (!playerRoom) {
                    console.log(`Jugador ${playerSymbol || 'Desconocido'} intentó mover sin estar en una sala.`);
                    return;
                }

                // Validar que sea el turno del jugador correcto
                if (playerRoom.turn !== playerSymbol) {
                    console.log(`Jugador ${playerSymbol} intentó mover pero no es su turno.`);
                    return;
                }

                const cellIdx = msg.cell;
                // Validar que la celda esté vacía
                if (playerRoom.board[cellIdx] !== null) {
                    console.log(`Celda ${cellIdx} ya está ocupada.`);
                    return;
                }

                // Impactamos el movimiento en el tablero real de la sala
                playerRoom.board[cellIdx] = playerSymbol;
                console.log(`Jugador ${playerSymbol} hizo un movimiento exitoso en la celda ${cellIdx}`);

                // Cambiamos el turno reactivamente
                playerRoom.turn = playerRoom.turn === 'X' ? 'O' : 'X';

                // "Empujamos" el nuevo estado de forma síncrona a ambos participantes
                playerRoom.players.forEach(p => {
                    if (p.connection.connected) {
                        p.connection.sendUTF(JSON.stringify({
                            type: 'state',
                            board: playerRoom.board,
                            turn: playerRoom.turn
                        }));
                    }
                });
                break;
                
            // ... (resto de los casos como 'rematch')
        }
    });

    // Manejar desconexiones inesperadas limpiamente sin bloquear recursos
    connection.on('close', function(reasonCode, description) {
        console.log((new Date()) + ' Peer ' + connection.remoteAddress + ' desconectado.');

        if (waitingPlayer && waitingPlayer.connection === connection) {
            waitingPlayer = null;
        }

        if (currentRoom) {
            // Notificar al oponente que se quedó solo
            const opponent = currentRoom.players.find(p => p.connection !== connection);
            if (opponent && opponent.connection.connected) {
                opponent.connection.sendUTF(JSON.stringify({ type: 'opponent_left' }));
            }
            // Eliminar la sala activa del índice de memoria
            rooms = rooms.filter(r => r !== currentRoom);
        }
    });
});