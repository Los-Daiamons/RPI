package com.daiamons;
<<<<<<< HEAD

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.concurrent.TimeUnit;
import java.lang.Runtime;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;
=======
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

>>>>>>> b8aa0aa69a7cec26e39eb8b3ae759adb46cf1ad3
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class RaspberryPiServer extends WebSocketServer {

    private static final int PORT = 8887;

    public RaspberryPiServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Enviar la IP de la WiFi al cliente cuando se abre la conexión
        String wifiIP = getWifiIP();
        conn.send("IP de la WiFi: " + wifiIP);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Limpiar el display cuando se cierra la conexión
        System.out.println("Conexión cerrada");
        // Aquí debes implementar la lógica para borrar la IP del display
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Manejar mensajes del cliente si es necesario
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado en el puerto " + getPort());
    }

    private String getWifiIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().contains("192.168.")) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "No se pudo obtener la IP de la WiFi";
    }

    public static void main(String[] args) {
        WebSocketServer server = new RaspberryPiServer();
        System.out.println("IP de la WiFi: " + ((RaspberryPiServer) server).getWifiIP());
<<<<<<< HEAD
        String ip = ""+((RaspberryPiServer) server).getWifiIP();
        server.start();

        System.out.println("Iniciando comando...");

        String directorio = "~/dev/rpi-rgb-led-matrix/";
        String comando = "text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse " + ip;

        try {
            // Construye el comando para cambiar de directorio y ejecutar el comando deseado
            String[] cmd = { "/bin/bash", "-c", "cd " + directorio + " && " + comando };

            // Objeto ProcessBuilder para construir y configurar el proceso
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);

            // Redirige los errores a la salida estándar
            processBuilder.redirectErrorStream(true);

            // Inicia el proceso
            Process p = processBuilder.start();

            // Lee la salida del proceso en un hilo separado
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Espera un tiempo (en este caso, 5 segundos)
            TimeUnit.SECONDS.sleep(5);

            // Destruye el proceso
            p.destroy();

            // Espera a que el proceso termine
            p.waitFor();

            // Comprueba el resultado de la ejecución
            System.out.println("Código de salida del comando: " + p.exitValue());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // finish
        System.out.println("Comandos finalizados.");
=======
        server.start();

>>>>>>> b8aa0aa69a7cec26e39eb8b3ae759adb46cf1ad3
    }
}
