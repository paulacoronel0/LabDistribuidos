//Trabajo Práctico Obligatorio 4, Lab. Prog. Distribuida, Programación Reactiva
// TATETI
//Hecho por: Paula Coronel, Antonio Sarmiento

const http = require('http');
const WebSocketServer = require('websocket').server;

// ---- Servidor HTTP ----
//Genera Servidor HTTP que responde 404 a todo. No sirve web, solo existe
//como base para que el WS se pueda utilizar sobre el servidor. 
const server = http.createServer(function(request, response) {
    console.log((new Date()) + ' HTTP ' + request.url);
    response.writeHead(404);
    response.end();
});
//escuchando nuevas peticiones
server.listen(8080, function() {
    console.log((new Date()) + ' Servidor escuchando en el puerto 8080');
});

// ---- WebSocket ----
//permite todas las conexiones entrantes. 
const wsServer = new WebSocketServer({
    httpServer: server,
    autoAcceptConnections: false 
});

function origenPermitido(origen) {
    return true;
}

// ---- Estado global ----
let salaActiva = null; //Controla si la sala ya está activa (limitamos solo a una sala activa el Tateti)
let jugadorEsperando = null; //guarda al primer jugador que se conecta, mientras espera rival. 
//let rooms = [];


// Permite encontrar la sala y símbolo de cualquier conexión en O(1),
// sin depender de closures ni de propiedades mutables en el objeto connection.
//ver que tanto nos sirve con una sola room
const jugadoresAceptados = new Map();

// ---- Lógica del juego ----

const JUGADAS_GANADORAS = [
    [0, 1, 2], [3, 4, 5], [6, 7, 8], //filas
    [0, 3, 6], [1, 4, 7], [2, 5, 8], //columnas
    [0, 4, 8], [2, 4, 6] //diagonales
];

function validarGanador(tablero) {
    for (const jugada of JUGADAS_GANADORAS) {
        const [a, b, c] = jugada;
        if (tablero[a] && tablero[a] === tablero[b] && tablero[a] === tablero[c]) {
            return { ganador: tablero[a], linea: jugada };
        }
    }
    if (tablero.every(celda => celda !== null)) {
        return { ganador: null, empate: true };
    }
    return null;
}

function enviar(conexion, obj) {
    if (conexion.connected) {
        conexion.sendUTF(JSON.stringify(obj));
    }
}

function broadcast(sala, obj) {
    sala.jugadores.forEach(p => enviar(p.conexion, obj));
}

function reiniciarTablero(sala) {
    sala.tablero        = Array(9).fill(null);
    sala.turno         = 'X';
    sala.votosRevancha = 0;
}

// ---- Conexiones ----

//cuando llega un nuevo evento de cliente intentando conectarse:
wsServer.on('request', function(request) {
   
    if (!origenPermitido(request.origin)) {
        request.reject();
        return;
    }

    const conexion = request.accept(null, request.origin);
    console.log((new Date()) + ' Jugador conectado: ' + conexion.remoteAddress);

     //se queda esperando evento de nuevo mensaje, hasta que el jugador interactúa:
    conexion.on('message', function(message) {
        if (message.type !== 'utf8') return;

        let msg;
        try { msg = JSON.parse(message.utf8Data); }
        catch (e) { return; }

        switch (msg.type) {
            
            case 'unirse_a_sala': {
                //si ya hay una partida en curso, no podrá unirse.
                if (salaActiva && jugadorEsperando === null) {
                    enviar(conexion, {
                        type: 'sala_llena'
                    });
                    return;
                }

                //no permite duplicados
                if (jugadorEsperando && jugadorEsperando.conexion === conexion) return;
                
                //si hay alguien esperando, se forma la sala
                if (jugadorEsperando && jugadorEsperando.conexion.connected) {
                    
                    const p1 = jugadorEsperando;
                    const p2 = { conexion: conexion, symbol: 'O' };

                    const sala = {
                        jugadores:      [p1, p2],
                        tablero:        Array(9).fill(null),
                        turno:         'X',
                        votosRevancha: 0
                    };
                    salaActiva = sala; //guardamos la sala
                    //rooms.push(sala);
                    jugadorEsperando = null;

                    // Registrar AMBAS conexiones en el Map
                    jugadoresAceptados.set(p1.conexion, { sala: sala, simbolo: 'X' });
                    jugadoresAceptados.set(p2.conexion, { sala: sala, simbolo: 'O' });

                    // Notificar individualmente (cada uno necesita saber su símbolo)
                    enviar(p1.conexion, { type: 'empezar', board: sala.tablero, turn: sala.turno, symbol: 'X' });
                    enviar(p2.conexion, { type: 'empezar', board: sala.tablero, turn: sala.turno, symbol: 'O' });

                    console.log('Sala creada: X vs O');
                
                //si no hay un jugador esperando, el jugador queda como nuevo jugador en espera.        
                } else {
                    jugadorEsperando = { conexion: conexion, simbolo: 'X' };
                    jugadoresAceptados.set(conexion, { sala: null, simbolo: 'X' });
                    enviar(conexion, { type: 'esperando', symbol: 'X' });
                    console.log('Jugador X esperando oponente...');
                }
                break;
            }

            case 'mov': {
                //busca al jugador
                const jugador = jugadoresAceptados.get(conexion);
                if (!jugador || !jugador.sala) return;
                
                const { sala, simbolo } = jugador;

                //verifica que sea su turno
                if (sala.turno !== simbolo) {
                    enviar(conexion, { type: 'error', msg: 'No es tu turno' });
                    return;
                }

                //verifica que la celda sea posición válida
                const pos = msg.cell;
                if (pos < 0 || pos > 8 || sala.tablero[pos] !== null) {
                    enviar(conexion, { type: 'error', msg: 'Celda inválida u ocupada' });
                    return;
                }
                //si pasa con éxito, entonces colocamos el movimiento en el tablero
                sala.tablero[pos] = simbolo;
                console.log(`Jugador ${simbolo} jugó en celda ${pos}`);

                //validamos si hay ganador
                const resultado = validarGanador(sala.tablero);

                //Si lo hay, juego terminado
                if (resultado) {
                    broadcast(sala, {
                        type:   'fin_juego',
                        board:  sala.tablero,
                        winner: resultado.ganador || null,
                        line:   resultado.linea   || null
                    });
                    console.log(resultado.ganador ? `Ganó ${resultado.ganador}` : 'Empate');
                } else {
                    //sino, cambiamos de turno para que juegue el otro jugador
                    sala.turno = sala.turno === 'X' ? 'O' : 'X';
                    broadcast(sala, { type: 'estado', board: sala.tablero, turn: sala.turno });
                }
                break;
            }

            case 'revancha': {
                //cuenta votos para la revancha, si llega a 2, reinicia el tablero
                const jugador = jugadoresAceptados.get(conexion);
                if (!jugador || !jugador.sala) return;

                const { sala } = jugador;
                sala.votosRevancha++;
                console.log(`Votos de revancha: ${sala.votosRevancha}/2`);

                if (sala.votosRevancha >= 2) {
                    reiniciarTablero(sala);
                    enviar(sala.jugadores[0].conexion, { type: 'empezar', board: sala.tablero, turn: sala.turno, symbol: 'X' });
                    enviar(sala.jugadores[1].conexion, { type: 'empezar', board: sala.tablero, turn: sala.turno, symbol: 'O' });
                    console.log('Revancha iniciada');
                } else {
                    //en caso contrario, sigue esperando
                    enviar(conexion, { type: 'esperando', symbol: jugador.simbolo });
                }
                break;
            }
        }
    });

    //cuando ocurre evento de cierre (se desconecta el jugador):
    conexion.on('close', function() {
        console.log((new Date()) + ' Jugador desconectado: ' + conexion.remoteAddress);

        //si se va el primer jugador (y no vino nadie más), entonces setea null de nuevo (para evitar salas con conexiones muertas)    
        if (jugadorEsperando && jugadorEsperando.conexion === conexion) {
            jugadorEsperando = null;
        }

        //busca jugador si estaba en partida
        const jugador = jugadoresAceptados.get(conexion);
        //en caso de encontrar la sala, se avisa al oponente que su rival se desconectó.
        if (jugador && jugador.sala) {
            const sala     = jugador.sala;
            const oponente = sala.jugadores.find(j => j.conexion !== conexion);
            if (oponente && oponente.conexion.connected) {
                enviar(oponente.conexion, { type: 'oponente_desconectado' });
                // el sobreviviente vuelve a la cola de espera
                jugadorEsperando = {
                    conexion: oponente.conexion,
                    simbolo: 'X'
                };

                jugadoresAceptados.set(oponente.conexion, {
                    sala: null,
                    simbolo: 'X'
                });

            }
            //rooms = rooms.filter(r => r !== room);
            salaActiva = null;
            jugadoresAceptados.delete(conexion);
        }
        
    });
});