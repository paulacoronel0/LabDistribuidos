
import java.io.*;
import java.net.*;
import java.util.logging.*;

//genera hilos para responder de forma concurrente a cada cliente
public class ServidorCentral {

    public static void main(String args[]) throws IOException {
        ServerSocket ss;
        System.out.print("Inicializando servidor... ");
        final int PORT_SERVER = 5000;
        final int PORT_SERVER_H = 5001;
        final int PORT_SERVER_P = 5002;
        //inicializo ambos sockets de los servidores horoscopo, pronostico
        ServerSocket s_horoscopo = new ServerSocket(PORT_SERVER_H);
        ServerSocket s_pronostico = new ServerSocket(PORT_SERVER_P);
        System.out.print("Configurando servidores horoscopo, pronostico... ");
        try {
            //abro socket del servidor
            ss = new ServerSocket(PORT_SERVER);
            System.out.println("\t[OK]");
            int idSession = 0;

            //Abrimos servidores de horoscopo, pronostico (global para todos los clientes)
            ((ServidorH) new ServidorH(s_horoscopo, PORT_SERVER_H)).start();
            ((ServidorP) new ServidorP(s_pronostico, PORT_SERVER_P)).start();

            while (true) {
                Socket socket;
                socket = ss.accept(); //aceptamos la conexión, redirigimos
                System.out.println("Nueva conexión entrante: " + socket);

                //Server buscará info para cada cliente 
                ((ServidorHilo) new ServidorHilo(socket, PORT_SERVER_H, PORT_SERVER_P, idSession)).start();
                idSession++;
            }
        } catch (IOException ex) {
            Logger.getLogger(ServidorCentral.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // cierra todos los server
        }
    }
}
