
/**
 * @author Paula Coronel, Antonio Sarmiento
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    public static final String IP_SERVIDOR_CENTRAL;
    public static final int PUERTO_SERVIDOR_CENTRAL;

    public static final String IP_SERVIDOR_HOROSCOPO;
    public static final int PUERTO_SERVIDOR_HOROSCOPO;

    public static final String IP_SERVIDOR_PRONOSTICO;
    public static final int PUERTO_SERVIDOR_PRONOSTICO;

    public static final long TIEMPO_VIDA_CACHE;
    public static final int SO_TIMEOUT_SERVIDOR_CENTRAL;

    static {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Error al leer config.properties: " + e.getMessage());
            System.exit(1);
        }

        IP_SERVIDOR_CENTRAL = props.getProperty("ip.central");
        PUERTO_SERVIDOR_CENTRAL = Integer.parseInt(props.getProperty("puerto.central"));

        IP_SERVIDOR_HOROSCOPO = props.getProperty("ip.horoscopo");
        PUERTO_SERVIDOR_HOROSCOPO = Integer.parseInt(props.getProperty("puerto.horoscopo"));

        IP_SERVIDOR_PRONOSTICO = props.getProperty("ip.pronostico");
        PUERTO_SERVIDOR_PRONOSTICO = Integer.parseInt(props.getProperty("puerto.pronostico"));

        TIEMPO_VIDA_CACHE = Long.parseLong(props.getProperty("cache.tiempo_vida"));
        SO_TIMEOUT_SERVIDOR_CENTRAL = Integer.parseInt(props.getProperty("servidor.so_timeout"));
    }
}
