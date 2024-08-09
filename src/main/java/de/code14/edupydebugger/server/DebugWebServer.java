package de.code14.edupydebugger.server;

import com.intellij.openapi.diagnostic.Logger;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * @author julian
 * @version 1.0
 * @since 03.07.24
 */
public class DebugWebServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebServer.class);

    private static HttpServer httpServer;
    private static boolean running = false;

    public static void startWebServer() {
        httpServer = new HttpServer();
        final NetworkListener networkListener = new NetworkListener("view", "localhost", 8026);
        httpServer.addListener(networkListener);

        httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(
                DebugWebServer.class.getClassLoader(), "/static/"
        ));

        try {
            httpServer.start();
            LOGGER.info("Web server started on port 8026");
            running = true;
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public static void stopWebServer() {
        if (httpServer != null && httpServer.isStarted()) {
            httpServer.shutdownNow();
            running = false;
        }
    }

    public static boolean isRunning() {
        return running;
    }
}
