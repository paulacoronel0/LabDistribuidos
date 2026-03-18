
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.logging.*;
import java.util.*;

//ServidorHilo actuará como INTERMEDIARIO entre la consulta del cliente y la respuesta de los servidores horoscopo, pronostico
public class ServidorHilo extends Thread {

    private int port_h; //puerto horoscopo
    private int port_p; //puerto pronostico
    private Socket socket_cliente; //socket del cliente
    private Socket socket_h;
    private Socket socket_p;
    private String ip;
    //private DataOutputStream dos;
    //private DataInputStream dis;
    BufferedReader input_cliente; //para leer lo que envie el cliente
    PrintStream output_cliente; //para imprimir datos de salida (cliente)
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
        this.ip = "localhost";

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

    public void desconnectar() {
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
        String request ;
        String[] consultas; //una consulta para cada server
        try {
            //si recibe nulo, quiere decir que el cliente cerró conexión
            while ((request = input_cliente.readLine()) != null) {
                try {
                    //se lee la petición del cliente
                    //request = input_cliente.readLine();
                    System.out.println("\tServidor " + id_session + "> recibio: " + request);
            
                    if (!request.contains(";")) {
                        output_cliente.println("\tServidor " + id_session + "> Error: Formato incorrecto. Use signo;fecha");
                        continue;
                    }
                    //se la divide
                    consultas = request.split(";");
                    //ej: "aries 25/12" "aries" "25/12"    
                    String signo = consultas[0].trim();
                    String fecha = consultas[1].trim();

                    if (!esSignoValido(signo)) {
                        output_cliente.println("\tServidor " + id_session + "> Error: El signo '" + signo + "' no es válido.");
                        continue;
                    }

                    if (!esFechaValida(fecha)) {
                        output_cliente.println("\tServidor " + id_session + "> Error: La fecha '" + fecha + "' es inválida o tiene formato incorrecto (use dd/mm/yyyy).");
                        continue;
                    }
                    //Para consulta 1 (horoscopo):
                    //envia petición al server encargado del horoscopo.
                    output_horoscopo.println(signo);
                    output_horoscopo.flush(); // Asegura que el dato salga hacia ServidorH
                    //captura respuesta e imprime (debug)
                    String respuesta_h = input_horoscopo.readLine();
                    if (respuesta_h != null) {
                        System.out.println("\tServidor " + id_session + "> " + respuesta_h);
                    }

                    //Para consulta 2 (pronostico):
                    //envia petición al server encargado del pronostico.
                    output_pronostico.println(fecha);
                    output_pronostico.flush(); // Asegura que el dato salga hacia ServidorP
                    //captura respuesta e imprime (debug)
                    String respuesta_p = input_pronostico.readLine();
                    if (respuesta_p != null) {
                        System.out.println("\tServidor " + id_session + "> " + respuesta_p);
                    }
                    // recibir respuestas 
                    String respuesta = respuesta_h + "- " + respuesta_p;
                    // enviar al cliente respuesta
                    output_cliente.flush();//vacia contenido
                    output_cliente.println(respuesta);

                } catch (IOException ex) {
                    Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("Cliente desconectado: " + id_session);
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

        }

        desconnectar();
    }
    

    public static boolean esSignoValido(String signo) {
        if (signo == null) return false;
        // Quitamos espacios y pasamos a minúsculas para comparar
        return SIGNOS_VALIDOS.contains(signo.trim().toLowerCase());
    }

    public static boolean esFechaValida(String fecha) {
        if (fecha == null) return false;
        
        // El formato que esperas del cliente
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withResolverStyle(ResolverStyle.SMART); // Estricto para evitar 31/02
        
        try {
            LocalDate.parse(fecha.trim(), formato);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
