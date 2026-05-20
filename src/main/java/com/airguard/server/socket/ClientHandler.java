package com.airguard.server.socket;

import com.airguard.server.service.AirSpaceService; // <-- ייבוא חדש
import com.airguard.server.entity.Plane;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private AirSpaceService airSpaceService; // <-- משתנה חדש לשירות
    private Gson gson = new Gson();

    // מעדכנים את הבנאי כדי לקבל את השירות
    public ClientHandler(Socket socket, AirSpaceService service) {
        this.clientSocket = socket;
        this.airSpaceService = service;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String jsonMessage;

            while ((jsonMessage = in.readLine()) != null) {
                // --- הוספנו את השורה הזו: ---
                System.out.println("🔍 RAW RECEIVED: " + jsonMessage);
                // ---------------------------

                try {
                    // כאן הוא נכשל כרגע, אבל לפחות נראה מה הגיע בשורה למעלה
                    Plane plane = gson.fromJson(jsonMessage, Plane.class);
                    airSpaceService.updatePlane(plane);
                    System.out.println("✅ Parsed: " + plane.getId());
                } catch (Exception e) {
                    System.out.println("❌ Error parsing: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Plane Disconnected.");
        }
    }
}