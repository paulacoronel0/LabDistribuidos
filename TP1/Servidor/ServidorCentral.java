
/**
 * @author Paula Coronel, Antonio Sarmiento
 */
import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ServidorCentral {

    // Genera hilos para responder de forma concurrente a cada cliente
    public static void main(String args[]) throws IOException {
        ServerSocket ss = null;
        System.out.print("Inicializando servidor... ");
        // Inicializo ambos sockets de los servidores horoscopo, pronostico
        ServerSocket s_horoscopo = new ServerSocket(Config.PUERTO_SERVIDOR_HOROSCOPO);
        ServerSocket s_pronostico = new ServerSocket(Config.PUERTO_SERVIDOR_PRONOSTICO);
        System.out.print("Configurando servidores horoscopo, pronostico... ");
        try {
            // Abro socket del servidor
            ss = new ServerSocket(Config.PUERTO_SERVIDOR_CENTRAL);

            System.out.println("\t[OK]");
            int idSession = 0;

            //Abrimos servidores de horoscopo, pronostico
            ((ServidorH) new ServidorH(s_horoscopo, Config.PUERTO_SERVIDOR_HOROSCOPO)).start();
            ((ServidorP) new ServidorP(s_pronostico, Config.PUERTO_SERVIDOR_PRONOSTICO)).start();
            //Estos servidores estarán "escuchando" hasta que llegue un nuevo cliente
            while (true) {
                try {
                    Socket socket;
                    ss.setSoTimeout(Config.SO_TIMEOUT_SERVIDOR_CENTRAL); //5 segundos esperando nuevas conexiones
                    socket = ss.accept(); // Aceptamos la conexión, redirigimos hacia un nuevo hilo que controle la petición.
                    System.out.println("Nueva conexión entrante: " + socket);

                    //Este Servidor buscará la información en los otros servidores para cada cliente 
                    ((ServidorHilo) new ServidorHilo(socket, Config.PUERTO_SERVIDOR_HOROSCOPO, Config.PUERTO_SERVIDOR_PRONOSTICO, idSession)).start();
                    idSession++;
                } catch (SocketTimeoutException e) {
                    System.out.println("Sin actividad por " + (Config.SO_TIMEOUT_SERVIDOR_CENTRAL / 1000) + " segundos. Cerrando servidor...");
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ServidorCentral.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (ss != null) {
                ss.close();
            }

            if (s_horoscopo != null) {
                s_horoscopo.close();
            }

            if (s_pronostico != null) {
                s_pronostico.close();
            }
        }
    }
}
