package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
public class LoginSimulation extends Simulation {

    HttpProtocolBuilder httpProtocolBuilder= http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");
    FeederBuilder<String> users=csv("users.csv").circular();

    ScenarioBuilder scn=scenario("Concurrent Login test")

            .feed(users)
            .exec(
                    http("User Login")
                            .post("/api/v1/auth/login")
                            .body(
                                    StringBody(
                                            """
                                              {
                                              "email":"#{email}",
                                              "password":"#{password}"
                                            }      
                                            """
                                    )
                            )
                            .check(status().is(200))
                            .check(
                                    jsonPath("$.data.accessToken").saveAs("jwt")
                            )
            );
    {
        setUp(
                scn.injectOpen(
                        rampUsers(300).during(12)
                )
        ).protocols(httpProtocolBuilder);
    }

}
