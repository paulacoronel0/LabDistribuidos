import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.logging.*;

public class ServidorHilo extends Thread {

    //ServidorHilo actuará como "INTERMEDIARIO" entre la consulta del cliente y la respuesta de los servidores horoscopo y pronostico
    private int port_h; //puerto horoscopo
    private int port_p; //puerto pronostico
    private Socket socket_cliente; //socket del cliente
    private Socket socket_h;
    private Socket socket_p;
    private String ip;

    BufferedReader input_cliente; //para leer lo que envie el cliente
    PrintStream output_cliente; //para enviar respuesta al cliente
    BufferedReader input_horoscopo; //para leer lo que envie el server horoscopo
    PrintStream output_horoscopo; //para enviar al server horoscopo
    BufferedReader input_pronostico; //para leer lo que envie el server pronostico
    PrintStream output_pronostico; //para enviar al server pronostico
    private int id_session; //identificador de la conexión

    private static final Set<String> SIGNOS_VALIDOS = new HashSet<>(Arrays.asList(
            "aries", "tauro", "geminis", "cancer", "leo", "virgo",
            "libra", "escorpio", "sagitario", "capricornio", "acuario", "piscis"
    ));

    public ServidorHilo(Socket socket_cliente, int port_h, int port_p, int id) {
        this.port_h = port_h;
        this.port_p = port_p;
        this.socket_cliente = socket_cliente;
        this.id_session = id;
        this.ip = Config.IP_SERVIDOR_CENTRAL; 

        try {
            this.socket_h = new Socket(this.ip, port_h);
            this.socket_p = new Socket(this.ip, port_p);
            this.input_cliente = new BufferedReader(new InputStreamReader(socket_cliente.getInputStream()));
            this.output_cliente = new PrintStream(socket_cliente.getOutputStream());
            this.input_horoscopo = new BufferedReader(new InputStreamReader(socket_h.getInputStream()));
            this.output_horoscopo = new PrintStream(socket_h.getOutputStream());
            this.input_pronostico = new BufferedReader(new InputStreamReader(socket_p.getInputStream()));
            this.output_pronostico = new PrintStream(socket_p.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void desconectar() {
        try {
            socket_cliente.close();
            socket_h.close();
            socket_p.close(); //cierro ambos
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        String request;
        String[] consultas; //una consulta para cada server
        try {
            // Si recibe nulo, quiere decir que el cliente cerró conexión
            while ((request = input_cliente.readLine()) != null) {
                try {
                    //Se lee la petición del cliente
                    System.out.println("\tServidor " + id_session + "> recibio: " + request);

                    if (!request.contains(";")) {
                        output_cliente.println("\tServidor " + id_session + "> Error: Formato incorrecto. Use signo;fecha");
                        continue;
                    }
                    // Se divide la petición
                    consultas = request.split(";");
                    // Ej: "aries 25/12" "aries" "25/12"    
                    String signo = consultas[0].trim();
                    String fecha = consultas[1].trim();
                    /* Luego, verificamos que el signo y la fecha sean válidos 
                    (en nuestro caso, no deberia pasar, pero será útil si dejamos que el cliente ingrese cualquier solicitud)*/
                    if (!esSignoValido(signo)) {
                        output_cliente.println("\tServidor " + id_session + "> Error: El signo '" + signo + "' no es válido.");
                        continue;
                    }
                    if (!esFechaValida(fecha)) {
                        output_cliente.println("\tServidor " + id_session + "> Error: La fecha '" + fecha + "' es inválida o tiene formato incorrecto (use dd/mm/yyyy).");
                        continue;
                    }
                    //Para consulta 1 (horoscopo):
                    // Envia petición al server encargado del horoscopo.
                    output_horoscopo.println(signo);
                    output_horoscopo.flush(); // fuerza el envio inmediato de datos hacia ServidorH
                    // Captura respuesta e imprime (debug)
                    String respuesta_h = input_horoscopo.readLine();
                    if (respuesta_h != null) {
                        System.out.println("\tServidor " + id_session + "> " + respuesta_h);
                    }

                    // Para consulta 2 (pronostico):
                    // Envia petición al server encargado del pronostico.
                    output_pronostico.println(fecha);
                    output_pronostico.flush(); // fuerza el envio inmediato de datos hacia ServidorP
                    // Captura respuesta e imprime (debug)
                    String respuesta_p = input_pronostico.readLine();
                    if (respuesta_p != null) {
                        System.out.println("\tServidor " + id_session + "> " + respuesta_p);
                    }
                    // Recibe y combina ambas respuestas
                    String respuesta = respuesta_h + "- " + respuesta_p;
                    // Luego, envia al cliente la respuesta completa
                    output_cliente.flush();// fuerza el envio inmediato de datos hacia el cliente
                    output_cliente.println(respuesta);

                } catch (IOException ex) {
                    Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("Cliente desconectado: " + id_session);
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            desconectar();
        }

    }

    //Métodos de validación:
    
    public static boolean esSignoValido(String signo) {
        if (signo == null) {
            return false;
        }
        // quitamos espacios y pasamos a minúsculas para comparar
        return SIGNOS_VALIDOS.contains(signo.trim().toLowerCase());
    }

    public static boolean esFechaValida(String fecha) {
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
