import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache {
    // Cache thread-safe para múltiples clientes
    private static Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    // Clase interna para almacenar respuesta y timestamp
    static class CachedResponse {
        String response;
        long timestamp;

        CachedResponse(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        // Verificar si el cache expiró (ej: 5 minutos)
        boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - timestamp) > ttlMillis;
        }
    }

    public static String processRequest(String request) {
        long TTL = 5 * 60 * 1000; // 5 minutos en milisegundos

        // Verificar si está en cache y no expiró
        if (cache.containsKey(request)) {
            CachedResponse cached = cache.get(request);
            if (!cached.isExpired(TTL)) {
                System.out.println("✓ Devolviendo desde CACHE: " + request);
                return cached.response;
            } else {
                // Cache expirado, remover
                cache.remove(request);
            }
        }

        // No está en cache, procesar la consulta
        System.out.println("⚙ PROCESANDO consulta: " + request);
        String response = fetchData(request); // Simula consulta costosa

        // Guardar en cache
        cache.put(request, new CachedResponse(response));

        return response;
    }

    // Simula obtener datos (clima, horóscopo, etc)
    private static String fetchData(String request) {
        try {
            Thread.sleep(2000); // Simula operación costosa
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (request.contains("clima")) {
            return "Temperatura: 25°C, Soleado";
        } else if (request.contains("horoscopo")) {
            return "Aries: Hoy es un buen día para nuevos proyectos";
        } else {
            return "Consulta no reconocida";
        }
    }
}