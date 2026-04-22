import java.rmi.Naming;

public class ServidorH {
    public static void main(String[] args) {
        try {
            Naming.rebind(
                    "rmi://" + args[1] + ":" + args[0] + "/ServicioH",
                    new ServicioH());
        } catch (Exception e) {
            System.err.println("Error en ServidorH: " + e.getMessage());
        }
    }
}
