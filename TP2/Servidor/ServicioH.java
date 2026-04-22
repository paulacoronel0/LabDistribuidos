
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ServicioH extends UnicastRemoteObject implements ServicioPrediccion {
    //Define la lógica del servicio horoscopo (buscar la respuesta, caché)

    private final String[] horoscopo = {
        "Enfoque en proyectos personales; éxito inminente.",
        "Momento de introspección: evita decisiones impulsivas.",
        "Nuevas oportunidades laborales en el horizonte cercano.",
        "Prioriza tu salud; el descanso será tu mejor aliado.",
        "Conexiones inesperadas traerán claridad a tus dudas.",
        "Período de abundancia creativa: aprovecha el impulso.",
        "Mantén la calma ante desafíos burocráticos hoy."};

    private ArrayList<String> horoscopoList;

    // Caché para consultas repetidas de horoscopo que permite concurrencia
    private static final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final long TIEMPO_VIDA = Config.TIEMPO_VIDA_CACHE; // 60.000 ms = 1 minuto, 2min la caché almacenará el dato.

    public ServicioH() throws RemoteException {
        super();
        this.horoscopoList = new ArrayList<>();
        Collections.addAll(this.horoscopoList, this.horoscopo);
    }

    @Override
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

        // BORRAR LUEGO, PRUEBA CACHÉ
        if (entrada != null) {
            System.out.println("\nServidorHoroscopo> Dato CACHÉ: " + llave + "-" + entrada.respuesta);
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
            return (System.currentTimeMillis() - this.tiempoCreacion) > ttl;
        }
    }
}
