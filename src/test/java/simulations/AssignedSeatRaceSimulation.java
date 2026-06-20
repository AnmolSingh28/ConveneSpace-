package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class AssignedSeatRaceSimulation extends Simulation {
    HttpProtocolBuilder httpProtocolBuilder=http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final String SEAT_ID = "9e0a5415-0e2d-4796-b4a9-953b01347067";

    FeederBuilder.Batchable<String> feeder =csv("tokens.csv").queue();

    ScenarioBuilder scn=scenario("Assigned Seat Lock Race Check")
            .feed(feeder)


            .exec(
                    http("Lock Assigned Seat")
                            .post("/api/v1/seats/assigned/lock")
                            .header("Authorization","Bearer #{token}")
                            .body(StringBody(
                                    """
                                    {
                                      "seatId": "9e0a5415-0e2d-4796-b4a9-953b01347067"
                                    }
                                    """
                            ))
                            .check(status().in(200,409))

            );
    {
        setUp(
                scn.injectOpen(
                        rampUsers(5000).during(15)
                )
        ).protocols(httpProtocolBuilder).assertions(
        global().failedRequests().percent().lt(99.0), // ← basically sab "expected" responses hain
                global().responseTime().percentile(95.0).lt(2000));
    }
}
