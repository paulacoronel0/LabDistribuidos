
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
                clientSocket = socket.accept();
                //una vez la acepta, genera un nuevo hilo que se encargará de responder al cliente
                //(para poder realizar consultas en simultáneo de varios clientes)
                new Thread(new ManejadorH(clientSocket, idSession)).start();
                idSession++;

            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public String prediccion(String request) {
        //Este método se encarga de devolver una respuesta (puede ser desde la caché o generada)

        String llave = request.trim().toLowerCase();

        // 1. Intentamos obtener la entrada
        EntradaCache entrada = cache.get(llave);

        // 2. Si existe, verificamos si expiró
        if (entrada != null && entrada.expiro(this.TIEMPO_VIDA)) {
            System.out.println("ServidorHoroscopo> Cache expirada para: " + llave + ". Eliminando...");
            cache.remove(llave); // Si expiró el tiempo, se remueve.
            entrada = null;      // Luego, forzamos a que lo genere de nuevo si fuera el caso.
        }

        //BORRAR LUEGO, PRUEBA CACHÉ
        if (entrada != null) {
            System.out.println("\nServidorHoroscopo> Dato CACHÉ: " + llave + "-" + entrada);
        }

        // 3. Si no existía (o lo acabamos de borrar por expirar)
        if (entrada == null) {
            Collections.shuffle(horoscopoList); //ordenamos aleatoriamente la lista
            String nuevaFrase = horoscopoList.get(0); //retornamos cualquier valor

            entrada = new EntradaCache(nuevaFrase); //generamos nueva entrada (guardada en caché)
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
                    //en caso de que la solicitud tuviera un formato incorrecto
                    if (solicitud.trim().isEmpty()) {
                        continue;
                    }

                    System.out.println("ServidorHoroscopo> Procesando: " + solicitud + " <Cliente " + this.id + ">");
                    //retornamos resultado (desde la caché o generado)
                    String resultado = prediccion(solicitud);

                    System.out.println("ServidorHoroscopo> Resultado de petición" + " <Cliente " + this.id + ">");
                    System.out.println("ServidorHoroscopo> \"" + solicitud + "\"");

                    output.println(resultado);
                    output.flush(); // Forzamos el envio al ServidorHilo
                }
                System.out.println("ServidorHoroscopo> Cliente cierra la conexión." + " <Cliente " + this.id + ">");
            } catch (IOException ex) {
                System.getLogger(ServidorH.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            } finally {
                try {
                    //cierra conexión
                    socket.close();
                } catch (IOException ex) {
                    System.getLogger(ServidorH.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        }
    }

}
