package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
public class BrowseSimulation extends Simulation {
    HttpProtocolBuilder httpProtocolBuilder=http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling Browse test");

    FeederBuilder.Batchable<String> feeder =csv("tokens.csv").queue();
    ScenarioBuilder browseConcerts=scenario("Browse concerts")
            .feed(feeder)
            .exec(
                    http("Get Upcoming Concerts")
                            .get("/api/v1/concerts")
                            .queryParam("page","0")
                            .queryParam("size","9")
                            .check(status().is(200))
            )
                    .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Featured Concerts")
                            .get("/api/v1/concerts/featured")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))

            .exec(
                    http("Search Concerts")
                            .get("/api/v1/concerts/search")
                            .queryParam("query","Concert")
                            .check(status().is(200))
            );
    {
        setUp(
                browseConcerts.injectOpen(
                        rampUsers(10000).during(Duration.ofSeconds(10))
                )
        ).protocols(httpProtocolBuilder)
                .assertions(
                        global().failedRequests().percent().lt(1.0),
                        global().responseTime().percentile(95).lt(2000)
                );
    }
}
