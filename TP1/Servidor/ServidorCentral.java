
import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ServidorCentral {

    //genera hilos para responder de forma concurrente a cada cliente
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

            //Abrimos servidores de horoscopo, pronostico
            ((ServidorH) new ServidorH(s_horoscopo, PORT_SERVER_H)).start();
            ((ServidorP) new ServidorP(s_pronostico, PORT_SERVER_P)).start();
            //estos estarán "escuchando" hasta que llegue un nuevo cliente
            while (true) {
                Socket socket;
                socket = ss.accept(); //aceptamos la conexión, redirigimos hacia un nuevo hilo que controle la petición.
                System.out.println("Nueva conexión entrante: " + socket);

                //Este Servidor buscará la información en los otros servidores para cada cliente 
                ((ServidorHilo) new ServidorHilo(socket, PORT_SERVER_H, PORT_SERVER_P, idSession)).start();
                idSession++;
            }
        } catch (IOException ex) {
            Logger.getLogger(ServidorCentral.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
