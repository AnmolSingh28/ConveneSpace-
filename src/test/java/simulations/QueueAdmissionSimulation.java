package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class QueueAdmissionSimulation extends Simulation {
    private static final String TIER_ID = "259e6dca-44db-4860-a714-cf194f972936";
    private static final String BASE_URL = "http://localhost:8080";

    HttpProtocolBuilder httpProtocolBuilder = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    FeederBuilder.FileBased<String> tokenFeeder = csv("tokens.csv").queue();

    ScenarioBuilder admissionTest = scenario("Queue Admission Test")
            .feed(tokenFeeder)
            .exec(
                    http("Join Queue")
                            .post("/api/v1/queue/join/" + TIER_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().is(200))
            )
            .repeat(30).on(
                    pause(2)
                            .exec(
                                    http("Check Admission")
                                            .get("/api/v1/queue/has-token/" + TIER_ID)
                                            .header("Authorization", "Bearer #{token}")
                                            .check(status().is(200))
                                            .check(jsonPath("$.data").saveAs("admitted"))
                            )
                            .doIf(session ->
                                    "true".equals(session.getString("admitted"))
                            ).then(
                                    exec(session -> {
                                        System.out.println(
                                                "ADMITTED USER"
                                        );
                                        return session;
                                    })
                            )
            );
    {
        setUp(
                admissionTest.injectOpen(
                        rampUsers(100).during(10)
                )
        ).protocols(httpProtocolBuilder);
    }

}
