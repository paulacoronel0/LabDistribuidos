import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorH extends Thread{

    private ServerSocket socket;
    BufferedReader input_cliente; //para leer lo que envie el cliente
    PrintStream output_cliente; //para imprimir datos de salida (cliente)
    private int puerto;
    
    String[] horoscopo = {
        "Enfoque en proyectos personales; éxito inminente.",
        "Momento de introspección: evita decisiones impulsivas.",
        "Nuevas oportunidades laborales en el horizonte cercano.",
        "Prioriza tu salud; el descanso será tu mejor aliado.",
        "Conexiones inesperadas traerán claridad a tus dudas.",
        "Período de abundancia creativa: aprovecha el impulso.",
        "Mantén la calma ante desafíos burocráticos hoy."};
    ArrayList<String> horoscopoList = new ArrayList<String>();
    private static final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final long TIEMPO_VIDA = 5 * 60 * 1000; // 60.000 ms = 1 minuto
    
    public ServidorH(ServerSocket socket, int puerto) {
        this.socket = socket;
        this.puerto = puerto;
        Collections.addAll(horoscopoList, horoscopo);   
        //try {
            
        //} catch (IOException ex) {
        //    Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        //}
    }
    
    @Override
    public void run() {
        
        try {
            System.out.println("ServidorHoroscopo> Servidor iniciado");    
            System.out.println("ServidorHoroscopo> En espera de cliente...");    
            while(true){
                //en espera de conexion, si existe la acepta
                Socket clientSocket = socket.accept();  // averiguar si es bloqueante, y sino como hacer 
                System.out.println("ServidorHoroscopo> Conexión aceptada");
                //Para leer lo que envie el cliente
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                
                //para imprimir datos de salida                
                PrintStream output = new PrintStream(clientSocket.getOutputStream());
                //se lee peticion del cliente
                //String request = input.readLine();
                //System.out.println("Cliente(id)> petición [" + request +  "]");
                //se procesa la peticion y se espera resultado
                
                //HACER LO DEL SERVERCACHE
                //String strOutput = prediccion(request);

                //Se imprime en consola "servidor"
                //System.out.println("ServidorHoroscopo> Resultado de petición");                    
                //System.out.println("ServidorHoroscopo> \"" + strOutput + "\"");
                //se imprime en cliente
                //output.flush();  //vacia contenido
                //output.println(strOutput);   
                
                // OPCION 2
                String request;
                // Bucle de persistencia: se queda aquí mientras SC mande datos
                while ((request = input.readLine()) != null) {
                    if (request.trim().isEmpty()) continue;
                    
                    System.out.println("ServidorHoroscopo> Procesando: " + request);
                    String result = prediccion(request);

                    System.out.println("ServidorHoroscopo> Resultado de petición");                    
                    System.out.println("ServidorHoroscopo> \"" + result + "\"");
                    
                    output.println(result);
                    output.flush(); // Forzamos la salida inmediata
                }

                System.out.println("ServidorHoroscopo> El intermediario cerró la conexión.");
                //cierra conexion
                clientSocket.close();
            }    
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        desconnectar();
    }
    
    public void desconnectar() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String prediccion(String request) {
        String llave = request.trim().toLowerCase();
        
        // 1. Intentamos obtener la entrada
        EntradaCache entrada = cache.get(llave);

        // 2. Si existe, verificamos si expiró
        if (entrada != null && entrada.expiro(TIEMPO_VIDA)) {
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
    
}
