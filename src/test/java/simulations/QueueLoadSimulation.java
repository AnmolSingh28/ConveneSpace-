package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
public class QueueLoadSimulation extends Simulation {

    private static final String TIER_ID = "259e6dca-44db-4860-a714-cf194f972936";
    private static final String BASE_URL = "http://localhost:8080";

    HttpProtocolBuilder httpProtocolBuilder = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    FeederBuilder.FileBased<String> tokenFeeder = csv("tokens.csv").queue();

    ScenarioBuilder loadTest = scenario("B Queue Load Test")
            .feed(tokenFeeder)
            .exec(
                    http("Join Queue")
                            .post("/api/v1/queue/join/" + TIER_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(5), Duration.ofSeconds(10))
            .exec(
                    http("Check Position")
                            .get("/api/v1/queue/position/" + TIER_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().in(200, 404))
            )
            .pause(Duration.ofSeconds(2))
            .exec(
                    http("Leave Queue")
                            .delete("/api/v1/queue/leave/" + TIER_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().in(200, 404,429))
            );
    {
        setUp(

                loadTest.injectOpen(
                        atOnceUsers(1000)
                ).protocols(httpProtocolBuilder)
        );
    }
}
