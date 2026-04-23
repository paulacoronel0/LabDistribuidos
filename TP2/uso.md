USO – Sistema RMI (TP2)
📦 Requisitos
Java instalado (JDK)
Estar ubicado en la carpeta raíz del proyecto (TP2/)
Estructura con packages:
Compartido/
Servidor/
Cliente/

🔧 1. Compilación
Desde la carpeta raíz:
javac Compartido/*.java Cliente/*.java Servidor/*.java

Esto genera todos los .class necesarios.

🚀 2. Levantar el registry
En una terminal (ubicado en la carpeta raiz):
rmiregistry 5000

⚠️ Dejar esta terminal abierta.

🖥️ 3. Ejecutar servidores
En 2da terminal (carpeta raiz):
java Servidor.ServidorCentral

En 3era terminal (carpeta raiz):
java Servidor.ServidorH

En 4ta terminal (carpeta raiz):
java Servidor.ServidorP

Dejarlos ejecutando.

👤 4. Ejecutar el cliente
En 5ta terminal:
java Cliente.Cliente


📊 Flujo esperado
El cliente realiza consultas al ServicioCentral
El servidor central consulta:
ServicioH (horóscopo)
ServicioP (pronóstico)
Se combinan las respuestas y se devuelven al cliente

⚠️ Problemas comunes
ClassNotFoundException / NoClassDefFoundError
→ ejecutar siempre desde la raíz del proyecto
NotBoundException
→ verificar nombres:
/ServicioCentral
/ServicioH
/ServicioP
Connection refused
→ asegurarse de que rmiregistry esté corriendo

✅ Notas
Todos los servicios usan el mismo puerto (5000)
Configuración centralizada en Config.java / config.properties
