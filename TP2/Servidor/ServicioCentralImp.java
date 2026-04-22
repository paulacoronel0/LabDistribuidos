import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.logging.*;

public class ServicioCentralImp extends UnicastRemoteObject implements ServicioCentral{

    private static final Set<String> SIGNOS_VALIDOS = new HashSet<>(Arrays.asList(
            "aries", "tauro", "geminis", "cancer", "leo", "virgo",
            "libra", "escorpio", "sagitario", "capricornio", "acuario", "piscis"
    ));
    
    public ServicioCentralImp() throws RemoteException {
        super();
    }


    @Override
    public String consultar(String signo, String fecha){
        String respuesta = "";
        //Se lee la petición del cliente
        /* Luego, verificamos que el signo y la fecha sean válidos 
        (en nuestro caso, no deberia pasar, pero será útil si dejamos que el cliente ingrese cualquier solicitud)*/
        if (!esSignoValido(signo)) {
            System.out.println("\tServidor> Error: El signo '" + signo + "' no es válido.");
            respuesta = "Error: El signo '" + signo + "' no es válido.";
            return respuesta;
        }
        if (!esFechaValida(fecha)) {
            System.out.println("\tServidor> Error: La fecha '" + fecha + "' es inválida o tiene formato incorrecto (use dd/mm/yyyy).");
            respuesta = "Error: La fecha '" + fecha + "' es inválida o tiene formato incorrecto (use dd/mm/yyyy).";
            return respuesta;
        }
        // Envia petición al servicio encargado del horoscopo.
        //if (args.length!=1) { System.err.println("Uso: Servidor Puerto"); return; }
        //    if (System.getSecurityManager() == null) {
        	// System.setSecurityManager(new RMISecurityManager()); 
	    //   	System.setProperty("java.rmi.server.hostname","localhost");}
        
        
        try {
            // Para consulta 1 (horoscopo):
            ServicioPrediccion servicioHConsulta = (ServicioPrediccion) Naming.lookup(
                    "//" + Config.IP_SERVIDOR_HOROSCOPO + ":" + Config.PUERTO_SERVIDOR_HOROSCOPO + "/ServicioH"); 
            String respuesta_h = servicioHConsulta.prediccion(signo);
            if (respuesta_h != null) {
                System.out.println("\tServidor >" + respuesta_h);
            }
            // Para consulta 2 (pronostico):
            ServicioPrediccion servicioPConsulta = (ServicioPrediccion) Naming.lookup(
                    "//" + Config.IP_SERVIDOR_PRONOSTICO + ":" + Config.PUERTO_SERVIDOR_PRONOSTICO + "/ServicioP");
            String respuesta_p = servicioPConsulta.prediccion(fecha);
            if (respuesta_p != null) {
                System.out.println("\tServidor >" + respuesta_p);
            }
            //Luego, combina las respuestas de ambos servicios
            respuesta = respuesta_h + " - " + respuesta_p;

        } catch (RemoteException e) {
            System.err.println("Error de comunicacion: " + e.toString());
            System.exit(1); 
        } catch (Exception e) {
            System.err.println("Excepcion en ServicioCentralImp:");
            e.printStackTrace();
            System.exit(1); 
        }
        return respuesta;
    }
    
    //Métodos de validación:
    
    private static boolean esSignoValido(String signo) {
        if (signo == null) {
            return false;
        }
        // quitamos espacios y pasamos a minúsculas para comparar
        return SIGNOS_VALIDOS.contains(signo.trim().toLowerCase());
    }

    private static boolean esFechaValida(String fecha) {
        if (fecha == null) {
            return false;
        }

        // formato esperado:
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withResolverStyle(ResolverStyle.SMART); // evitar resultados como '31/02'

        try {
            LocalDate.parse(fecha.trim(), formato);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
