package simulations;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import io.gatling.javaapi.core.Simulation;

public class FifoSimulation extends Simulation {

    private static final String TIER_ID = "259e6dca-44db-4860-a714-cf194f972936";
    private static final String BASE_URL = "http://localhost:8080";

    HttpProtocolBuilder httpProtocolBuilder = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    FeederBuilder.FileBased<String> tokenFeeder = csv("tokens.csv").queue();

    ScenarioBuilder fifoTest = scenario("Fifo Correctness")
            .feed(tokenFeeder)
            .exec(
                    http("Join Queue")
                            .post("/api/v1/queue/join/" + TIER_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().is(200))
                            .check(jsonPath("$.data.position").saveAs("position"))
            )
            .exec(session -> {
                System.out.println("User joined at position: " + session.getString("position"));
                return session;
            })
            .repeat(20).on(
                    pause(3)
                            .exec(
                                    http("Poll Token")
                                            .get("/api/v1/queue/has-token/" + TIER_ID)
                                            .header("Authorization", "Bearer #{token}")
                                            .check(status().is(200))
                                            .check(jsonPath("$.data").saveAs("hasToken"))
                            )
                            .doIf(session -> "true".equals(session.getString("hasToken"))).then(
                                    exec(
                                            http("Lock GA Ticket")
                                                    .post("/api/v1/seats/ga/" + TIER_ID + "/lock")
                                                    .header("Authorization", "Bearer #{token}")
                                                    .body(StringBody("{\"quantity\": 1}"))
                                                    .check(status().in(200, 201, 409))
                                                    .check(status().saveAs("lockStatus"))
                                    )
                            )
            );

    {
        setUp(
                // Test A — 5 users FIFO, staggered joins
                fifoTest.injectOpen(
                        rampUsers(200).during(5)
                ).protocols(httpProtocolBuilder)
        );
    }
}
