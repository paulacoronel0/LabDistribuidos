
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

    public Persona(int id) {
        this.id = id;
        this.PORT = 5000;
        this.IP_SERVER = "localhost";
    }

    @Override
    public void run() {
        boolean exit = false;//bandera para controlar ciclo del programa
        //Socket socket;//Socket para la comunicacion cliente servidor        
        try {
            System.out.println("Cliente" + (id) + "> Inicio");
            socket = new Socket(IP_SERVER, PORT); //abre socket  
            while (!exit) {//ciclo repetitivo                                

                //Para leer lo que envie el servidor      
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //para imprimir datos del servidor
                output = new PrintStream(socket.getOutputStream());
                //Para leer lo que escriba el usuario            
                brRequest = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Cliente" + (id) + "Escriba comando");
                //captura comando escrito por el usuario
                String request = brRequest.readLine();    // cambiarlo para que sea aleatorio por automatico            
                //manda peticion al servidor
                output.println(request);
                //captura respuesta e imprime
                String st = input.readLine();
                if (st != null) {
                    System.out.println("Servidor> " + st);
                }
                if (request.equals("exit")) {//terminar aplicacion
                    exit = true;
                    System.out.println("Cliente" + (id) + " Fin de programa");
                }
            }//end while

        } catch (IOException ex) {
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
        for (int i = 0; i < 5; i++) {
            clients.add(new Persona(i));
        }
        for (Thread thread : clients) {
            thread.start();
        }
    }
}
