import java.io.*;
import java.net.*;
import java.util.logging.*;

//intermediario
public class Servidor {
    public static void main(String args[]) throws IOException {
        ServerSocket ss;
        System.out.print("Inicializando servidor... ");
        try {
            ss = new ServerSocket(10578);
            System.out.println("\t[OK]");
            int idSession = 0;
            //abrir serverH, serverP aca
            (ServidorH) new ServidorH(socket_h, puertoH)).start();
            (ServidorP) new ServidorP(socket_p, puertoP)).start();
            while (true) {
                Socket socket;
                socket = ss.accept();
                System.out.println("Nueva conexión entrante: " + socket);
                
                ((ServidorHilo) new ServidorHilo(socket_h, socket_p, idSession, puertoH, puertoP)).start();
                idSession++;
            }
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        } finally{
            // cierra todos los server
        }
    }
}

// CLIENTE --> SERVIDOR (INTERMEDIARIO) --> SERVERHILO (funcione como cliente de los SH y SC, pero servidor CLIENTE) --> SERVER_H , SERVER_C
// SERVER_H , SERVER_P (Tienen caché)

//idea
//if (time_since_last_request > TIMEOUT) {
    //cerrar_servidor();
//}