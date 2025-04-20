package com.hyp.playground;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class RiderLocationFetcher {

    private static final String API_URL = "https://api.pidge.in/v1.0/store/tracking/rider-location?id=1743862703891REAAM6R8";
    private static double lastLatitude;
    private static double lastLongitude;

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable task = () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject json = new JSONObject(response.toString()).getJSONObject("data");
                    Double latitude = json.getDouble("latitude");
                    Double longitude = json.getDouble("longitude");

                    // Check if location changed
                    if (!latitude.equals(lastLatitude) || !longitude.equals(lastLongitude)) {
                        System.out.println("--------------------------------------------------");
                        System.out.println("Time: " + LocalDateTime.now());
                        System.out.println("Latitude: " + latitude);
                        System.out.println("Longitude: " + longitude);
                        lastLatitude = latitude;
                        lastLongitude = longitude;
                    }

                } else {
                    System.out.println("Failed to fetch. HTTP Code: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
    }
}