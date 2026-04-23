// interface que contiene los métodos del servicio central
package Compartido;
import java.rmi.*;

public interface ServicioCentral extends Remote {
    String consultar(String signo, String fecha) throws RemoteException;
}