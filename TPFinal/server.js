const http = require('http');
const WebSocketServer = require('websocket').server;

// ── Servidor HTTP ─────────────────────────────────────────────────

const server = http.createServer(function(request, response) {
    console.log((new Date()) + ' HTTP ' + request.url);
    response.writeHead(404);
    response.end();
});

server.listen(8080, function() {
    console.log((new Date()) + ' Servidor escuchando en el puerto 8080');
});

// ── WebSocket ─────────────────────────────────────────────────────

const wsServer = new WebSocketServer({
    httpServer: server,
    autoAcceptConnections: false
});

function originIsAllowed(origin) {
    return true;
}

// ── Estado global ─────────────────────────────────────────────────

let waitingPlayer = null;
let rooms = [];

// Map<connection, { room, symbol }>
// Permite encontrar la sala y símbolo de cualquier conexión en O(1),
// sin depender de closures ni de propiedades mutables en el objeto connection.
const connectionState = new Map();

// ── Lógica del juego ──────────────────────────────────────────────

const WINNING_COMBOS = [
    [0, 1, 2], [3, 4, 5], [6, 7, 8],
    [0, 3, 6], [1, 4, 7], [2, 5, 8],
    [0, 4, 8], [2, 4, 6]
];

function checkWinner(board) {
    for (const combo of WINNING_COMBOS) {
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

function send(connection, obj) {
    if (connection.connected) {
        connection.sendUTF(JSON.stringify(obj));
    }
}

function broadcast(room, obj) {
    room.players.forEach(p => send(p.connection, obj));
}

function resetRoom(room) {
    room.board        = Array(9).fill(null);
    room.turn         = 'X';
    room.rematchVotes = 0;
}

// ── Conexiones ────────────────────────────────────────────────────

wsServer.on('request', function(request) {
    if (!originIsAllowed(request.origin)) {
        request.reject();
        return;
    }

    const connection = request.accept(null, request.origin);
    console.log((new Date()) + ' Jugador conectado: ' + connection.remoteAddress);

    connection.on('message', function(message) {
        if (message.type !== 'utf8') return;

        let msg;
        try { msg = JSON.parse(message.utf8Data); }
        catch (e) { return; }

        switch (msg.type) {

            case 'join_room': {
                if (waitingPlayer && waitingPlayer.connection === connection) return;

                if (waitingPlayer && waitingPlayer.connection.connected) {
                    // Segundo jugador: armamos la sala
                    const p1 = waitingPlayer;
                    const p2 = { connection, symbol: 'O' };

                    const room = {
                        players:      [p1, p2],
                        board:        Array(9).fill(null),
                        turn:         'X',
                        rematchVotes: 0
                    };

                    rooms.push(room);
                    waitingPlayer = null;

                    // Registrar AMBAS conexiones en el Map
                    connectionState.set(p1.connection, { room, symbol: 'X' });
                    connectionState.set(p2.connection, { room, symbol: 'O' });

                    // Notificar individualmente (cada uno necesita saber su símbolo)
                    send(p1.connection, { type: 'game_start', board: room.board, turn: room.turn, symbol: 'X' });
                    send(p2.connection, { type: 'game_start', board: room.board, turn: room.turn, symbol: 'O' });

                    console.log('Sala creada: X vs O');

                } else {
                    // Primer jugador: queda en el lobby
                    waitingPlayer = { connection, symbol: 'X' };
                    connectionState.set(connection, { room: null, symbol: 'X' });
                    send(connection, { type: 'waiting', symbol: 'X' });
                    console.log('Jugador X esperando oponente...');
                }
                break;
            }

            case 'move': {
                const state = connectionState.get(connection);
                if (!state || !state.room) return;

                const { room, symbol } = state;

                if (room.turn !== symbol) {
                    send(connection, { type: 'error', msg: 'No es tu turno' });
                    return;
                }

                const idx = msg.cell;
                if (idx < 0 || idx > 8 || room.board[idx] !== null) {
                    send(connection, { type: 'error', msg: 'Celda inválida u ocupada' });
                    return;
                }

                room.board[idx] = symbol;
                console.log(`Jugador ${symbol} jugó en celda ${idx}`);

                const result = checkWinner(room.board);

                if (result) {
                    broadcast(room, {
                        type:   'game_over',
                        board:  room.board,
                        winner: result.winner || null,
                        line:   result.line   || null
                    });
                    console.log(result.winner ? `Ganó ${result.winner}` : 'Empate');
                } else {
                    room.turn = room.turn === 'X' ? 'O' : 'X';
                    broadcast(room, { type: 'state', board: room.board, turn: room.turn });
                }
                break;
            }

            case 'rematch': {
                const state = connectionState.get(connection);
                if (!state || !state.room) return;

                const { room } = state;
                room.rematchVotes++;
                console.log(`Votos de revancha: ${room.rematchVotes}/2`);

                if (room.rematchVotes >= 2) {
                    resetRoom(room);
                    send(room.players[0].connection, { type: 'game_start', board: room.board, turn: room.turn, symbol: 'X' });
                    send(room.players[1].connection, { type: 'game_start', board: room.board, turn: room.turn, symbol: 'O' });
                    console.log('Revancha iniciada');
                } else {
                    send(connection, { type: 'waiting', symbol: state.symbol });
                }
                break;
            }
        }
    });

    connection.on('close', function() {
        console.log((new Date()) + ' Jugador desconectado: ' + connection.remoteAddress);

        if (waitingPlayer && waitingPlayer.connection === connection) {
            waitingPlayer = null;
        }

        const state = connectionState.get(connection);
        if (state && state.room) {
            const room     = state.room;
            const opponent = room.players.find(p => p.connection !== connection);
            if (opponent && opponent.connection.connected) {
                send(opponent.connection, { type: 'opponent_left' });
            }
            rooms = rooms.filter(r => r !== room);
        }

        connectionState.delete(connection);
    });
});