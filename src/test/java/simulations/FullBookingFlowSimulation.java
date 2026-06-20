package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class FullBookingFlowSimulation extends Simulation {

    private static final String TIER_ID = "259e6dca-44db-4860-a714-cf194f972936";
    private static final int TOTAL_USERS = 10000;
    private static final int RAMP_SECONDS = 6;
    private static final int MAX_POLL_ATTEMPTS = 60;

    HttpProtocolBuilder httpProtocolBuilder = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .header("Accept-Encoding", "gzip, deflate");

    FeederBuilder.Batchable<String> tokenFeeder = csv("tokens.csv").queue();

    //STEP 1: Join Queue
    ChainBuilder joinQueue = exec(
            http("1 — Join Queue")
                    .post("/api/v1/queue/join/" + TIER_ID)
                    .header("Authorization", "Bearer #{token}")
                    .check(status().in(200, 409))
    ).pause(Duration.ofMillis(200));

    //STEP 2: Poll for admission token
    ChainBuilder pollForAdmission = repeat(MAX_POLL_ATTEMPTS, "pollAttempt").on(
            doIf(session -> !"ADMITTED".equals(session.getString("admissionStatus")))
                    .then(
                            exec(
                                    http("2 — Poll Admission Token")
                                            .get("/api/v1/queue/has-token/" + TIER_ID)
                                            .header("Authorization", "Bearer #{token}")
                                            .check(status().is(200))
                                            .check(jsonPath("$.data").saveAs("admitted"))
                            ).pause(Duration.ofSeconds(2))
                                    .doIf(session -> "true".equals(session.getString("admitted")))
                                    .then(
                                            exec(session -> session.set("admissionStatus", "ADMITTED"))
                                    )
                    )
    );

    //STEP 3: Lock GA Ticket
    ChainBuilder lockGaTicket = doIf(
            session -> "ADMITTED".equals(session.getString("admissionStatus"))
    ).then(
            exec(
                    http("3 — Lock GA Ticket")
                            .post("/api/v1/seats/ga/" + TIER_ID + "/lock")
                            .header("Authorization", "Bearer #{token}")
                            .body(StringBody("""
                                    { "quantity": 1 }
                                    """))
                            .check(status().in(200, 201, 400, 409, 429))
                            // ← string mein save karo manually
                            .check(status().transform(s -> String.valueOf(s)).saveAs("lockStatus"))
            ).pause(Duration.ofMillis(300))
    );

    // STEP 4: Create Booking
    ChainBuilder createBooking = doIf(
            session -> "200".equals(session.getString("lockStatus"))
                    || "201".equals(session.getString("lockStatus"))
    ).then(
            exec(session -> session
                    .set("idempotencyKey", UUID.randomUUID().toString())
                    .set("bookingId", "")
                    .set("bookingStatus", "")
            ).exec(
                    http("4 — Create Booking")
                            .post("/api/v1/bookings")
                            .header("Authorization", "Bearer #{token}")
                            .body(StringBody(session ->
                                    """
                                    {
                                        "tierId": "%s",
                                        "seatIds": [],
                                        "quantity": 1,
                                        "idempotencyKey": "%s"
                                    }
                                    """.formatted(TIER_ID, session.getString("idempotencyKey"))
                            ))
                            .check(status().in(200, 201, 400, 409))
                            .check(status().transform(s -> String.valueOf(s)).saveAs("bookingStatus"))
                            .check(jsonPath("$.data.id").optional().saveAs("bookingId"))
            ).pause(Duration.ofMillis(600))
    );

    // ── STEP 5: Create Payment Order ───────────────────────────────────────
    ChainBuilder createPaymentOrder = doIf(
            session -> {
                String bookingStatus = session.getString("bookingStatus");
                String bookingId = session.getString("bookingId");
                return ("200".equals(bookingStatus) || "201".equals(bookingStatus))
                        && bookingId != null
                        && !bookingId.isBlank();
            }
    ).then(
            exec(
                    http("5 — Create Payment Order")
                            .post("/api/v1/payments/create-order")
                            .header("Authorization", "Bearer #{token}")
                            .body(StringBody(session ->
                                    """
                                    { "bookingId": "%s" }
                                    """.formatted(session.getString("bookingId"))
                            ))
                            .check(status().in(200, 201, 400, 409))
                            .check(status().transform(s -> String.valueOf(s)).saveAs("paymentOrderStatus"))
                            .check(jsonPath("$.data.id").optional().saveAs("paymentId"))
            ).pause(Duration.ofMillis(300))
    );

    // ── STEP 6: Simulate Payment Success ──────────────────────────────────
    ChainBuilder simulatePaymentSuccess = doIf(
            session -> {
                String paymentOrderStatus = session.getString("paymentOrderStatus");
                String bookingId = session.getString("bookingId");
                return "200".equals(paymentOrderStatus)
                        && bookingId != null
                        && !bookingId.isBlank();
            }
    ).then(
            exec(
                    http("6 — Simulate Payment Success")
                            .post(session ->
                                    "/api/v1/payments/simulate-success/" +
                                            session.getString("bookingId")
                            )
                            .header("Authorization", "Bearer #{token}")
                            .check(status().in(200, 400, 409))
            ).pause(Duration.ofMillis(500))
    );

    // ── STEP 7: Get Payment Status ─────────────────────────────────────────
    ChainBuilder getPaymentStatus = doIf(
            session -> {
                String paymentOrderStatus = session.getString("paymentOrderStatus");
                String bookingId = session.getString("bookingId");
                return "200".equals(paymentOrderStatus)
                        && bookingId != null
                        && !bookingId.isBlank();
            }
    ).then(
            exec(
                    http("7 — Get Payment Status")
                            .get(session ->
                                    "/api/v1/payments/booking/" + session.getString("bookingId")
                            )
                            .header("Authorization", "Bearer #{token}")
                            .check(status().in(200, 404, 429))
            )
    );

    // ── SCENARIO ───────────────────────────────────────────────────────────
    ScenarioBuilder concertLaunch = scenario("Concert Launch — Full System Load")
            .feed(tokenFeeder)
            .exec(session -> session
                    .set("admissionStatus", "WAITING")
                    .set("lockStatus", "")
                    .set("bookingStatus", "")
                    .set("paymentOrderStatus", "")
                    .set("bookingId", "")
                    .set("paymentId", "")
            )
            .exec(joinQueue)
            .exec(pollForAdmission)
            .exec(lockGaTicket)
            .exec(createBooking)
            .exec(createPaymentOrder)
            .exec(simulatePaymentSuccess)
            .exec(getPaymentStatus);

    // ── SETUP ──────────────────────────────────────────────────────────────
    {
        setUp(
                concertLaunch.injectOpen(
                        rampUsers(TOTAL_USERS).during(Duration.ofSeconds(RAMP_SECONDS))
                )
        ).protocols(httpProtocolBuilder);
    }
}