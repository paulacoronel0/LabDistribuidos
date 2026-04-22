// interface que contiene los métodos del servicio prediccion (tanto para signo, como para fecha)

import java.rmi.*;

interface ServicioPrediccion extends Remote {
    String prediccion(String solicitud) throws RemoteException;
}