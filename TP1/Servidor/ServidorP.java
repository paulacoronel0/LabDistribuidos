
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorP extends Thread {

    private ServerSocket socket;
    private int puerto;

    private final String[] pronosticoClima = {
        "Despejado con ráfagas de viento norte; 22°C.",
        "Mayormente nublado, baja probabilidad de chaparrones; 15°C.",
        "Cielo limpio y descenso térmico hacia la noche; 10°C.",
        "Inestabilidad climática con mejoras temporarias; 18°C.",
        "Ola de calor: máxima alcanzando los 34°C con humedad alta.",
        "Niebla matinal reduciendo visibilidad, luego soleado; 12°C.",
        "Tormentas aisladas con actividad eléctrica moderada."};

    private ArrayList<String> pronosticoClimaList;
    //caché para consultas repetidas de horoscopo
    private static final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final long TIEMPO_VIDA = 2 * 60 * 1000; // 60.000 ms = 1 minuto, 2min la caché almacenará el dato.

    public ServidorP(ServerSocket socket, int puerto) {
        this.socket = socket;
        this.puerto = puerto;
        this.pronosticoClimaList = new ArrayList<>();
        Collections.addAll(this.pronosticoClimaList, this.pronosticoClima);
    }

    @Override
    public void run() {

        try {
            System.out.println("ServidorPronostico> Servidor iniciado");
            System.out.println("ServidorPronostico> En espera de cliente...");
            //Socket de cliente
            Socket clientSocket;
            int idSession = 0; //para identificar conexión
            while (true) {
                //en espera de conexion, si existe la acepta
                clientSocket = socket.accept();  // averiguar si es bloqueante, y sino como hacer 
                new Thread(new ManejadorP(clientSocket, idSession)).start();
                idSession++;
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public String prediccion(String request) {
        String llave = request.trim().toLowerCase();

        // 1. Intentamos obtener la entrada
        EntradaCache entrada = cache.get(llave);

        // 2. Si existe, verificamos si expiró
        if (entrada != null && entrada.expiro(TIEMPO_VIDA)) {
            System.out.println("ServidorPronostico> Cache expirada para: " + llave + ". Eliminando...");
            cache.remove(llave); // LO ELIMINAMOS
            entrada = null;      // Forzamos a que entre al siguiente bloque
        }

        // 3. Si no existía (o lo acabamos de borrar por expirar)
        if (entrada == null) {
            Collections.shuffle(pronosticoClimaList);
            String nuevaFrase = pronosticoClimaList.get(0);

            entrada = new EntradaCache(nuevaFrase);
            cache.put(llave, entrada);
            System.out.println("ServidorPronostico> Nueva entrada creada para: " + llave);
        }

        return entrada.respuesta;
    }

    private static class EntradaCache {

        String respuesta;
        long tiempoCreacion;

        EntradaCache(String respuesta) {
            this.respuesta = respuesta;
            this.tiempoCreacion = System.currentTimeMillis();
        }

        // Verifica si pasaron más de X milisegundos
        boolean expiro(long ttl) {
            return (System.currentTimeMillis() - tiempoCreacion) > ttl;
        }
    }

    class ManejadorP implements Runnable {

        //se crea para cada cliente, para generar concurrencia en las consultas del pronostico
        private final Socket socket;
        private final int id;

        public ManejadorP(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
        }

        @Override

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream output = new PrintStream(socket.getOutputStream());
                String solicitud;
                while ((solicitud = input.readLine()) != null) {
                    if (solicitud.trim().isEmpty()) {
                        continue;
                    }

                    System.out.println("ServidorPronostico> Procesando: " + solicitud + " <Cliente " + this.id + ">");
                    String resultado = prediccion(solicitud);

                    System.out.println("ServidorPronostico> Resultado de petición" + " <Cliente " + this.id + ">");
                    System.out.println("ServidorPronostico> \"" + solicitud + "\"");

                    output.println(resultado);
                    output.flush(); // Forzamos la salida inmediata
                }
                System.out.println("ServidorPronostico> Cliente cierra la conexión." + " <Cliente " + this.id + ">");
            } catch (IOException ex) {
                System.getLogger(ServidorP.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            } finally {
                try {
                    //cierra conexion
                    socket.close();
                } catch (IOException ex) {
                    System.getLogger(ServidorP.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        }
    }

}
