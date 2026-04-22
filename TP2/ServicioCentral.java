// interface que contiene los métodos del servicio central

import java.rmi.*;

interface ServicioCentral extends Remote {
    String consultar(String signo, String fecha) throws RemoteException;
}