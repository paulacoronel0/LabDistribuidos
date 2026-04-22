
import java.rmi.Naming;

public class ServidorH {

    //Define el registro RMI del objeto Servicio Horoscopo
    public static void main(String[] args) {
        try {
            Naming.rebind(
                    "rmi://" + Config.IP_SERVIDOR_HOROSCOPO + ":" + Config.PUERTO_SERVIDOR_HOROSCOPO + "/ServicioH",
                    new ServicioH());
        } catch (Exception e) {
            System.err.println("Error en ServidorH: " + e.getMessage());
        }
    }
}
