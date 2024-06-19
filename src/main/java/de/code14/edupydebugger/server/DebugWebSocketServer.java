package de.code14.edupydebugger.server;


import org.glassfish.tyrus.server.Server;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class DebugWebSocketServer {

    private static Server server;

    public static void startServer() {
        server = new Server("localhost", 8025, "/websockets", null, DebugServerEndpoint.class);

        try {
            server.start();
            System.out.println("Server started on port " + server.getPort());
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

}
