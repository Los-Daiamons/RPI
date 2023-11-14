package com.daiamons;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;
import java.util.concurrent.ConcurrentHashMap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

public class RaspberryPiServer extends WebSocketServer {

    private static final int PORT = 8887;
    private Map<WebSocket, String> connectionNames = new ConcurrentHashMap<>();
    private static Process proc;
    private static Process mensaje;

    public RaspberryPiServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {

            System.out.println(conn.getResourceDescriptor());
            System.out.println("Nueva conexión: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
            connectionNames.put(conn, conn.getResourceDescriptor());
            updateAndSendConnectionCount();
            proc.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada");

        connectionNames.remove(conn);
        updateAndSendConnectionCount();

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String directorio = "~/dev/rpi-rgb-led-matrix/";
        // sudo ./led-matrix -t "Su mensaje aquí"
        System.out.println(message);

        if (mensaje != null) {
            mensaje.destroy();
        }

        String comando = "text-scroller -f ~/dev/bitmap-fonts/bitmap/gomme/Gomme10x20n.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse "
                + message;

        try {
            String[] cmd = { "/bin/bash", "-c", "cd " + directorio + " && " + comando };
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            mensaje = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(mensaje.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado en el puerto " + getPort());
    }

    private void updateAndSendConnectionCount() {
        int mobileConnections = 0;
        int desktopConnections = 0;

        for (String name : connectionNames.values()) {
            if (name.contains("mobile")) {
                mobileConnections++;
            } else if (name.contains("desktop")) {
                desktopConnections++;
            }
        }

        System.out.println("Conexiones móviles: " + mobileConnections);
        System.out.println("Conexiones de escritorio: " + desktopConnections);
        String message = "{\"type\": \"connection_count\", \"mobile_connections\": " + mobileConnections +
                ", \"desktop_connections\": " + desktopConnections + "}";
        broadcastt(message);
    }


    private void broadcastt(String message) {
        for (WebSocket client : connectionNames.keySet()) {
            client.send(message);
        }
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
        String ip = "" + ((RaspberryPiServer) server).getWifiIP();
        server.start();

        System.out.println("Iniciando comando...");

        String directorio = "~/dev/rpi-rgb-led-matrix/";
        // sudo ./led-matrix -t "Su mensaje aquí"
        String comando = "text-scroller -f ~/dev/bitmap-fonts/bitmap/gomme/Gomme10x20n.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse "
                + ip;

        try {
            // Construye el comando para cambiar de directorio y ejecutar el comando deseado
            String[] cmd = { "/bin/bash", "-c", "cd " + directorio + " && " + comando };

            // Objeto ProcessBuilder para construir y configurar el proceso
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);

            // Redirige los errores a la salida estándar
            processBuilder.redirectErrorStream(true);

            // Inicia el proceso
            proc = processBuilder.start();

            // Lee la salida del proceso en un hilo separado
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // finish
        System.out.println("Comandos finalizados.");

    }
}
