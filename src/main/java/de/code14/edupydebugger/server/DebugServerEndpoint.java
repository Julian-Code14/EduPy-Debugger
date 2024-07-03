package de.code14.edupydebugger.server;


import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;

/**
 * @author julian
 * @version 1.0
 * @since 19.06.24
 */
@WebListener
@ServerEndpoint(value = "/debug")
public class DebugServerEndpoint {

    @OnMessage
    public String onMessage(String message, Session session) {
        return message;
    }

}
