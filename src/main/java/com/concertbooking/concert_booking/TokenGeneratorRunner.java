package com.concertbooking.concert_booking;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.*;

public class TokenGeneratorRunner {

    private static final int TOTAL_USERS = 10000;
    private static final int THREADS = 50;

    public static void main(String[] args) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(THREADS);

        BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                "src/test/resources/tokens.csv",
                                false
                        )
                );

        writer.write("token\n");

        CountDownLatch latch =
                new CountDownLatch(TOTAL_USERS);

        for (int i = 1; i <= TOTAL_USERS; i++) {

            final int userNumber = i;

            executor.submit(() -> {

                try {

                    RestTemplate restTemplate =
                            new RestTemplate();

                    String email =
                            "test" + userNumber + "@gmail.com";

                    String requestBody = """
                            {
                              "email":"%s",
                              "password":"Yachi2802@@"
                            }
                            """.formatted(email);

                    HttpHeaders headers =
                            new HttpHeaders();

                    headers.setContentType(
                            MediaType.APPLICATION_JSON
                    );

                    HttpEntity<String> entity =
                            new HttpEntity<>(
                                    requestBody,
                                    headers
                            );

                    ResponseEntity<String> response =
                            restTemplate.postForEntity(
                                    "http://localhost:8080/api/v1/auth/login",
                                    entity,
                                    String.class
                            );

                    String body = response.getBody();

                    String token =
                            body.split("\"accessToken\":\"")[1]
                                    .split("\"")[0];

                    synchronized (writer) {
                        writer.write(token);
                        writer.newLine();
                    }

                    System.out.println(
                            "SUCCESS: " + email
                    );

                } catch (Exception e) {

                    System.out.println(
                            "FAILED: test"
                                    + userNumber
                                    + "@gmail.com"
                    );

                } finally {

                    latch.countDown();
                }
            });
        }

        latch.await();

        writer.close();

        executor.shutdown();

        System.out.println("DONE");
    }
}