import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;


public class ServicioP extends UnicastRemoteObject implements ServicioPrediccion{

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

    // Caché para consultas repetidas de pronostico
    private static final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final long TIEMPO_VIDA = Config.TIEMPO_VIDA_CACHE; // 60.000 ms = 1 minuto, durante 2min la caché almacenará el dato.

    public ServicioP() throws RemoteException {
        super();
        this.pronosticoClimaList = new ArrayList<>();
        Collections.addAll(this.pronosticoClimaList, this.pronosticoClima);
    }

    @Override
    public String prediccion(String request) {
        //Este método se encarga de devolver una respuesta (puede ser desde la caché o generada)

        String llave = request.trim().toLowerCase();

        // 1. Intentamos obtener la entrada
        EntradaCache entrada = cache.get(llave);

        // 2. Si existe, verificamos si expiró
        if (entrada != null && entrada.expiro(TIEMPO_VIDA)) {
            System.out.println("ServidorPronostico> Cache expirada para: " + llave + ". Eliminando...");
            cache.remove(llave); // Si expiró el tiempo, se remueve.
            entrada = null;      // Luego, forzamos a que lo genere de nuevo si fuera el caso.
        }

        //BORRAR LUEGO, PRUEBA CACHÉ
        if (entrada != null) {
            System.out.println("\nServidorHoroscopo> Dato CACHÉ: " + llave + "-" + entrada.respuesta);
        }

        // 3. Si no existía (o lo acabamos de borrar por expirar)
        if (entrada == null) {
            Collections.shuffle(pronosticoClimaList); //ordenamos aleatoriamente la lista
            String nuevaFrase = pronosticoClimaList.get(0); //retornamos cualquier valor

            entrada = new EntradaCache(nuevaFrase); //generamos nueva entrada (guardada en caché)
            cache.put(llave, entrada);
            System.out.println("ServidorPronostico> Nueva entrada creada para: " + llave);
        }

        return entrada.respuesta;
    }

    // Clase que controla la caché
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
