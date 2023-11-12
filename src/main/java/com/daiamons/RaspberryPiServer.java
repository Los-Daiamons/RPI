package com.daiamons;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;

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
        try {
            // Obtenemos el ID del proceso actual
            long pid = ProcessHandle.current().pid();
            
            // Enviamos la señal de interrupción al proceso actual
            ProcessBuilder processBuilder = new ProcessBuilder("kill", "-2", String.valueOf(pid));
            Process process = processBuilder.start();
            
            // Esperamos a que el proceso termine
            int exitCode = process.waitFor();
            
            // Imprimimos el código de salida del proceso
            System.out.println("Código de salida: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        String ip = ""+((RaspberryPiServer) server).getWifiIP();
        server.start();

        System.out.println("Iniciando comando...");

        String directorio = "~/dev/rpi-rgb-led-matrix/";
        // sudo ./led-matrix -t "Su mensaje aquí"
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


        } catch (IOException e) {
            e.printStackTrace();
        }

        // finish
        System.out.println("Comandos finalizados.");

    }
}
