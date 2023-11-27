package com.daiamons;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;
import java.util.concurrent.ConcurrentHashMap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class RaspberryPiServer extends WebSocketServer {

    private static final int PORT = 8887;
    private Map<WebSocket, String> connectionNames = new ConcurrentHashMap<>();
    private static Process proc;
    private static Process mensaje;
    private static String ip;

    private HashMap<String, String> users = new HashMap<>();

    public RaspberryPiServer() {
        super(new InetSocketAddress(PORT));
        loadUsers();

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            System.out.println(conn.getResourceDescriptor());
            System.out.println("Nueva conexión: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
            String name = conn.getResourceDescriptor() + "?" + generateRandomCombination();
            connectionNames.put(conn, name);
            System.out.println(connectionNames.get(conn));
            for (Map.Entry<WebSocket, String> entry : connectionNames.entrySet()) {
                WebSocket webSocket = entry.getKey();
                String namee = entry.getValue();
                System.out.println("WebSocket: " + webSocket + ", Name: " + namee);
            }
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

        if (message.startsWith("{\"type\":\"auth\"")) {
            // Parsear el mensaje de autenticación
            Map<String, String> authData = parseAuthenticationMessage(message);

            // Obtener el nombre de usuario y la contraseña
            String username = authData.get("username");
            String password = authData.get("password");

            // Verificar las credenciales
            if (checkUser(username, password)) {
                System.out.println("Autenticación exitosa para el usuario: " + username);
            } else {
                System.out.println("Autenticación fallida para el usuario: " + username);
                conn.close();
                return;
            }
            
        } else {
            String directorio = "~/dev/rpi-rgb-led-matrix/";
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

                System.out.println(reader.readLine());
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("Error al ejecutar el comando para mostrar el mensaje en el display usando el onMessage");
            }

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
            if (name.contains("?name=mobile")) {
                mobileConnections++;
            } else if (name.contains("?name=desktop")) {
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
        ip = "" + ((RaspberryPiServer) server).getWifiIP();
        server.start();

        mostrarIP(ip);

        System.out.println("Comandos finalizados.");

    }

    private static void mostrarIP(String ip) {
        String directorio = "~/dev/rpi-rgb-led-matrix/";
        // sudo ./led-matrix -t "Su mensaje aquí"
        String comando = "text-scroller -f ~/dev/bitmap-fonts/bitmap/gomme/Gomme10x20n.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse "
                + ip;

        try {
            String[] cmd = { "/bin/bash", "-c", "cd " + directorio + " && " + comando };

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);

            processBuilder.redirectErrorStream(true);

            proc = processBuilder.start();


        } catch (IOException e) {
            System.out.println("Error al ejecutar el comando inicial");
        }
    }

    public static String generateRandomCombination() {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int length = 8;
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    private void loadUsers() {
        try {
            // Lee el contenido del archivo JSON
            Path filePath = Paths.get("./src/main/java/com/daiamons/users.json");
            String jsonContent = new String(Files.readAllBytes(filePath));

            // Convierte el JSON a un HashMap
            JSONObject jsonObject = new JSONObject(jsonContent);
            for (String key : jsonObject.keySet()) {
                users.put(key, jsonObject.getString(key));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkUser(String user, String pass) {
        if (users.containsKey(user)) {
            if (users.get(user).equals(pass)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> parseAuthenticationMessage(String message) {
        Map<String, String> authData = new HashMap<>();
        JSONObject json = new JSONObject(message);
        authData.put("username", json.getString("username"));
        authData.put("password", json.getString("password"));
        return authData;
    }
}
