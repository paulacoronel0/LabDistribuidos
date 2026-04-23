
package Cliente;
import Compartido.Config;
import Compartido.ServicioCentral;
import java.io.*;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.*;

class Persona extends Thread {

    private final int PORT;
    private final String IP_SERVER;
    private int id; //identificador del cliente

    // Listas para generar peticiones aleatorias
    private final String[] SIGNOS = {
        "Aries", "Tauro", "Leo"
    };

    private final String[] FECHAS = {
        "10/01/2026", "15/05/2026"
    };

    public Persona(int id) {
        this.id = id;
        this.PORT = Config.PUERTO_SERVIDOR_CENTRAL;
        this.IP_SERVER = Config.IP_SERVIDOR_CENTRAL;
    }

    @Override
    public void run() {
        boolean exit = false;//bandera para controlar ciclo del programa
        Random random = new Random();
        try {
            System.out.println("Cliente" + (id) + "> Inicio");

            for (int i = 0; i < 3; i++) {
                // Generar petición aleatoria: "Signo;Fecha"
                String signoAleatorio = SIGNOS[random.nextInt(SIGNOS.length)];
                String fechaAleatoria = FECHAS[random.nextInt(FECHAS.length)];
                //la petición contiene lo pedido por el cliente y su identificador.

                System.out.println("Cliente " + id + "> Enviando: " + signoAleatorio + ";" + fechaAleatoria);

                // Obtiene el Servicio
                ServicioCentral servicioCentral = (ServicioCentral) Naming.lookup("//" + this.IP_SERVER + ":" + this.PORT + "/ServicioCentral");

                // Envia la consulta al ServicioCentral y obtiene la respuesta
                String respuesta = servicioCentral.consultar(signoAleatorio, fechaAleatoria);

                // Luego, lee la respuesta del ServidoCentral
                System.out.println("Cliente " + id + ">  Recibió: " + respuesta);

                // Pequeña pausa de 1 segundo entre peticiones para ver el flujo
                Thread.sleep(1000);
            }
            // Al finalizar las 3 peticiones, enviamos exit para cerrar
            System.out.println("Cliente " + id + "> Pruebas finalizadas. Enviando exit...");

        } catch (Exception ex) {
            Logger.getLogger(Persona.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

public class Cliente {

    public static void main(String[] args) {
        ArrayList<Thread> clients = new ArrayList<Thread>();
        //generamos los clientes y los ejecutamos
        for (int i = 0; i < 2; i++) {
            clients.add(new Persona(i));
        }
        for (Thread thread : clients) {
            thread.start();
        }
    }
}
