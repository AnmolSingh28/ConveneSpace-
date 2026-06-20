package simulations;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SeatLockRaceSimulation extends Simulation {
    HttpProtocolBuilder httpProtocolBuilder=http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final String TIER_ID= "e448ea6f-25d5-4cda-8e58-1762e68e988e";

    FeederBuilder.Batchable<String> feeder =csv("tokens.csv").queue();

    ScenarioBuilder scn=scenario("Seat Lock Race Check")
            .feed(feeder)


            //lock the same ga tier
            .exec(
                    http("Lock GA tier")
                            .post("/api/v1/seats/ga/" + TIER_ID + "/lock")
                            .header("Authorization","Bearer #{token}")
                            .body(StringBody(
                                    """
                                    {
                                      "quantity": 1
                                    }
                                    """
                            ))
                            .check(status().in(200,400,409,429,503))
            );
    {
        setUp(
                scn.injectOpen(
                        atOnceUsers(500)
                )
        ).protocols(httpProtocolBuilder)
                .assertions(
                        global().failedRequests()
                                .percent()
                                .lt(5.0),
                        global().responseTime()
                                .percentile(95.0)
                                .lt(5000)
                );
    }
}
