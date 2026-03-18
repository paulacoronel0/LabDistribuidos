
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;

class Persona extends Thread {

    protected Socket socket;
    protected BufferedReader input; // Buffer que lee el server
    protected PrintStream output; //Imprime datos del server
    private final int PORT;
    private final String IP_SERVER;
    //protected DataOutputStream dos; POR LAS DUDAS
    ////protected DataInputStream dis; es para enviar un solo dato y tipo de dato
    protected BufferedReader brRequest; // Buffer que lee lo que escribe el usuario
    private int id; //identificador del cliente

    // Listas para generar peticiones aleatorias
    private final String[] SIGNOS = {
        "Aries", "Tauro", "Geminis", "Cancer", "Leo", "Virgo", 
        "Libra", "Escorpio", "Sagitario", "Capricornio", "Acuario", "Piscis"
    };
    
    private final String[] FECHAS = {
        "10/01/2026", "15/05/2026", "20/12/2025", "01/01/2026", "28/02/2026"
    };

    public Persona(int id) {
        this.id = id;
        this.PORT = 5000;
        this.IP_SERVER = "localhost";
    }

    @Override
    public void run() {
        boolean exit = false;//bandera para controlar ciclo del programa
        Random random = new Random();   
        try {
            System.out.println("Cliente" + (id) + "> Inicio");
            socket = new Socket(IP_SERVER, PORT); //abre socket  
            //Para leer lo que envie el servidor      
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //para imprimir datos del servidor
            output = new PrintStream(socket.getOutputStream());
            //Para leer lo que escriba el usuario            
            brRequest = new BufferedReader(new InputStreamReader(System.in));
            // while (!exit) {//ciclo repetitivo                                
            //     System.out.println("Cliente" + (id) + "> Escriba comando");
            //     //captura comando escrito por el usuario
            //     String request = brRequest.readLine();    // cambiarlo para que sea aleatorio por automatico   
            //     if (request == null || request.isEmpty()) continue;         
            //     //manda peticion al servidor
            //     output.println(request);
            //     //captura respuesta e imprime
            //     String st = input.readLine();
            //     if (st != null) {
            //         System.out.println("Servidor> " + st);
            //     }
            //     if (request.equals("exit")) {//terminar aplicacion
            //         exit = true;
            //         System.out.println("Cliente" + (id) + " Fin de programa");
            //     }
            // }//end while
            // Vamos a enviar 5 peticiones automáticas por cada cliente
            for (int i = 0; i < 2; i++) {
                // Generar petición aleatoria: "Signo;Fecha"
                String signoAleatorio = SIGNOS[random.nextInt(SIGNOS.length)];
                String fechaAleatoria = FECHAS[random.nextInt(FECHAS.length)];
                String request = signoAleatorio + ";" + fechaAleatoria;

                System.out.println("Cliente " + id + "> Enviando: " + request);
                output.println(request);

                // Leer respuesta del Servidor Central
                String response = input.readLine();
                System.out.println("Cliente " + id + "> Recibió: " + response);

                // Pequeña pausa de 1 segundo entre peticiones para ver el flujo
                Thread.sleep(1000);
            }
            // Al finalizar las 5 peticiones, enviamos exit para cerrar
            output.println("exit");
            System.out.println("Cliente " + id + "> Pruebas finalizadas. Enviando exit...");

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Persona.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                socket.close(); //cierra el socket después del exit
            } catch (IOException ex) {
                Logger.getLogger(Persona.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

public class Cliente {

    public static void main(String[] args) {
        ArrayList<Thread> clients = new ArrayList<Thread>();
        for (int i = 0; i < 2; i++) {
            clients.add(new Persona(i));
        }
        for (Thread thread : clients) {
            thread.start();
        }
    }
}
