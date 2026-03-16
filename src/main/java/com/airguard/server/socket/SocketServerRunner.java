package com.airguard.server.socket;

import com.airguard.server.service.AirSpaceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class SocketServerRunner implements CommandLineRunner {

    private static final int PORT = 7000;
    private final AirSpaceService airSpaceService;

    public SocketServerRunner(AirSpaceService airSpaceService) {
        this.airSpaceService = airSpaceService;
    }

    @Override
    public void run(String... args) throws Exception {
        // פתיחת נתיב עוקף ל-Socket כדי לא לתקוע את ה-Spring
        new Thread(() -> {
            runServer();
        }).start();
    }

    private void runServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("✈️ AirGuard Socket Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✈️ New Plane Connected!");

                // עכשיו השורה הזו תעבוד (כי יצרנו את הקובץ!)
                ClientHandler handler = new ClientHandler(clientSocket, airSpaceService);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}