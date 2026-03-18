import java.io.*;
import java.net.*;
import java.util.logging.*;
public class ServidorHilo extends Thread {
    private Socket socket_h;
    private Socket socket_p;
    private DataOutputStream dos;
    private DataInputStream dis;
    private int idSessio;
    private int puertoH = 5001;
    private int puertoP = 5002;

    public ServidorHilo(Socket socket_h, Socket socket_p, int id, int puertoH, int puertoP) {
        this.socket_h = socket_h;
        this.socket_p = socket_p;
        this.idSessio = id;
        
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void desconnectar() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void run() {
        String accion = "";
        String acciones[] = accion.split(" ");
        try {
            //recupera accion
            //divide con split
            // "aries 25/12" "aries" "25/12"
            // mandar ambas solicitudes
            // recibir respuestas 
            // 
            // respuesta = respuesta_h, respuesta_p
            // enviar al cliente respuesta







            
            //socket_h = new Socket(SERVER, puertoH);  // Para consultar a los Servidores SH 
            //socket_p = new Socket(SERVER, puertoP);  // Para consultar a los Servidores SP
            //accion = dis.readUTF();
            //if(accion.equals("hola")){
                //System.out.println("El cliente con idSesion "+this.idSessio+" saluda");
                //dos.writeUTF(respuesta);
            //}
        } catch (IOException ex) {
            Logger.getLogger(ServidorHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
        desconnectar();
    }
}