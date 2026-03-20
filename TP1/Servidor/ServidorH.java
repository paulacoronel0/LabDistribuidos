
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorH extends Thread {

    private ServerSocket socket;
    private int puerto;

    private final String[] horoscopo = {
        "Enfoque en proyectos personales; éxito inminente.",
        "Momento de introspección: evita decisiones impulsivas.",
        "Nuevas oportunidades laborales en el horizonte cercano.",
        "Prioriza tu salud; el descanso será tu mejor aliado.",
        "Conexiones inesperadas traerán claridad a tus dudas.",
        "Período de abundancia creativa: aprovecha el impulso.",
        "Mantén la calma ante desafíos burocráticos hoy."};

    private ArrayList<String> horoscopoList;

    //caché para consultas repetidas de horoscopo
    private static final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final long TIEMPO_VIDA = 2 * 60 * 1000; // 60.000 ms = 1 minuto, 2min la caché almacenará el dato.

    public ServidorH(ServerSocket socket, int puerto) {
        this.socket = socket;
        this.puerto = puerto;
        this.horoscopoList = new ArrayList<>();
        Collections.addAll(this.horoscopoList, this.horoscopo);
    }

    @Override
    public void run() {

        try {
            System.out.println("ServidorHoroscopo> Servidor iniciado");
            System.out.println("ServidorHoroscopo> En espera de cliente...");
            //Socket de cliente
            Socket clientSocket;
            int idSession = 0; //para identificar conexión
            while (true) {
                //en espera de conexion, si existe la acepta
                clientSocket = socket.accept();  // averiguar si es bloqueante, y sino como hacer 
                new Thread(new ManejadorH(clientSocket, idSession)).start();
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
        if (entrada != null && entrada.expiro(this.TIEMPO_VIDA)) {
            System.out.println("ServidorHoroscopo> Cache expirada para: " + llave + ". Eliminando...");
            cache.remove(llave); // LO ELIMINAMOS
            entrada = null;      // Forzamos a que entre al siguiente bloque
        }

        // 3. Si no existía (o lo acabamos de borrar por expirar)
        if (entrada == null) {
            Collections.shuffle(horoscopoList);
            String nuevaFrase = horoscopoList.get(0);
            entrada = new EntradaCache(nuevaFrase);
            cache.put(llave, entrada);
            System.out.println("ServidorHoroscopo> Nueva entrada creada para: " + llave);
        }

        return entrada.respuesta;
    }

    //clase que controla la caché
    private static class EntradaCache {

        String respuesta;
        long tiempoCreacion;

        EntradaCache(String respuesta) {
            this.respuesta = respuesta;
            this.tiempoCreacion = System.currentTimeMillis();
        }

        // Verifica si pasaron más de X milisegundos
        boolean expiro(long ttl) {
            return (System.currentTimeMillis() - this.tiempoCreacion) > ttl;
        }
    }

    class ManejadorH implements Runnable {

        //se crea para cada cliente, para generar concurrencia en las consultas del horoscopo
        private final Socket socket;
        private final int id;

        public ManejadorH(Socket socket, int id) {
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

                    System.out.println("ServidorHoroscopo> Procesando: " + solicitud + " <Cliente " + this.id + ">");
                    String resultado = prediccion(solicitud);

                    System.out.println("ServidorHoroscopo> Resultado de petición" + " <Cliente " + this.id + ">");
                    System.out.println("ServidorHoroscopo> \"" + solicitud + "\"");

                    output.println(resultado);
                    output.flush(); // Forzamos la salida inmediata
                }
                System.out.println("ServidorHoroscopo> Cliente cierra la conexión." + " <Cliente " + this.id + ">");
            } catch (IOException ex) {
                System.getLogger(ServidorH.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            } finally {
                try {
                    //cierra conexion
                    socket.close();
                } catch (IOException ex) {
                    System.getLogger(ServidorH.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        }
    }

}
