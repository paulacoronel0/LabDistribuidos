
import java.rmi.Naming;

public class ServidorP {

    //Define el registro RMI del objeto Servicio Pronostico
    public static void main(String[] args) {
        try {
            Naming.rebind(
                    "rmi://" + Config.IP_SERVIDOR_PRONOSTICO + ":" + Config.PUERTO_SERVIDOR_PRONOSTICO + "/ServicioP",
                    new ServicioP());
        } catch (Exception e) {
            System.err.println("Error en ServidorH: " + e.getMessage());
        }
    }
}
