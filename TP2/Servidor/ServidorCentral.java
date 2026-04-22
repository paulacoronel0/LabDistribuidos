
import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.logging.*;

public class ServidorCentral {

    public static void main(String args[]) throws IOException {

        System.out.print("Inicializando servidor... ");
        String puerto = Integer.toString(Config.PUERTO_SERVIDOR_CENTRAL);
        System.setProperty(puerto, Config.IP_SERVIDOR_CENTRAL);

        try {
            ServicioCentral servicioCentral = new ServicioCentralImp();
            Naming.rebind("rmi://" + Config.IP_SERVIDOR_CENTRAL + ":" + Config.PUERTO_SERVIDOR_CENTRAL + "/ServicioCentral", servicioCentral);
            System.out.println("Servidor Central listo");
        } catch (RemoteException e) {
            System.err.println("Error de comunicacion: " + e.toString());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Excepcion en ServidorCentral:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
