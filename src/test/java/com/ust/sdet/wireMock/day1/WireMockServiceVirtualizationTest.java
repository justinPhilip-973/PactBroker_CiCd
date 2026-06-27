package com.ust.sdet.wireMock.day1;

//import java.io.
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class WireMockServiceVirtualizationTest {
    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().templatingEnabled(true))
            .build();

    private HttpClient client;

    @BeforeEach
    void wireMockAndClientSetUp(){
        io.restassured.RestAssured.baseURI = wiremock.baseUrl();
        client = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("GET /orders/{id} is virtualized and verified")
    void verifiesTheGetRequest(){
        wiremock.stubFor(get(urlPathEqualTo("/orders/123"))
                .willReturn(okJson("""
                        {"id": 123, "status":"CONFIRMED", "total":42.0}""")));

        given().when().get("/orders/123")
                .then()
        .statusCode(200).contentType(ContentType.JSON).body("id",equalTo( 123));
    }

    @Test
    @DisplayName("Exercise 1: Stub Inventory - two outcomes")
    void stubInventorycheckfor200and409(){
        wiremock.stubFor(get(urlPathEqualTo("/inventory/SKU-9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"sku":"SKU-9", "qty":5}
                                """)));

        wiremock.stubFor(get(urlPathEqualTo("/inventory/SKU-0"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error":"OUT_OF_STOCK"}""")));

        given().when().get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("sku",equalTo("SKU-9"))
                .body("qty", equalTo(5));

        given().when().get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .body("error",equalTo("OUT_OF_STOCK"));


        wiremock.verify(exactly(1),getRequestedFor(urlPathEqualTo("/inventory/SKU-9")));
        wiremock.verify(exactly(1),getRequestedFor(urlPathEqualTo("/inventory/SKU-0")));

    }


    @Test
    @DisplayName("Exercise 2: Make it slow - forcing a timeout")
    void makeStubSlowByForcingTimeout() throws Exception{
        wiremock.stubFor(get(urlPathEqualTo("/orders/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"sku":"SKU-9", "qty":5, "time":"delayed"}
                        """)
                        .withFixedDelay(3000)
                ));

        HttpRequest failRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseURI+"/orders/slow"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

        assertThrows(HttpTimeoutException.class,
                ()-> client.send(failRequest,HttpResponse.BodyHandlers.ofString()));

        given().when().get("/orders/slow")
                .then()
                .statusCode(200)
                .body("time",equalTo("delayed"));


        HttpRequest successRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseURI+"/orders/slow"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(successRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("delayed"));
    }


    @Test
    @DisplayName("Exercise 3: Make it stateful - PENDING then CONFIRMED")
    void makeStubStatefulPendingThenConfirmed(){
        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                        .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"PENDING"}"""))
                .willSetStateTo("CONFIRMED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("CONFIRMED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"CONFIRMED"}""")));

        given().when().get("/orders/42").then().statusCode(200)
                .body("id", equalTo(42))
                .body("Status",equalTo("PENDING"));
        given().when().get("/orders/42").then().statusCode(200)
                .body("id", equalTo(42))
                .body("Status",equalTo("CONFIRMED"));

        wiremock.verify(exactly(2),getRequestedFor(urlPathEqualTo("/orders/42")));
    }

    @Test
    @DisplayName("Exercise 4: Make it stateful - 202(Accepted) PENDING -> 200(Ok) " +
            "CONFIRMED -> 500(server error) FAILED")
    void makeStubStatefulPendingConfirmedFailed(){
        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"PENDING"}"""))
                .willSetStateTo("CONFIRMED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("CONFIRMED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"CONFIRMED"}"""))
                .willSetStateTo("SHIPPED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("SHIPPED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"SHIPPED"}"""))
                .willSetStateTo("DISPATCHED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("DISPATCHED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"DISPATCHED"}"""))
                .willSetStateTo("FAILED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("FAILED")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"FAILED"}""")));

        given().when().get("/orders/42")
                .then()
                .statusCode(202)
                .body("Status", equalTo("PENDING"));
        given().when().get("/orders/42")
                .then()
                .statusCode(200)
                .body("Status", equalTo("CONFIRMED"));
        given().when().get("/orders/42")
                .then()
                .statusCode(200)
                .body("Status", equalTo("SHIPPED"));
        given().when().get("/orders/42")
                .then()
                .statusCode(200)
                .body("Status", equalTo("DISPATCHED"));
        given().when().get("/orders/42")
                .then()
                .statusCode(500)
                .body("Status", equalTo("FAILED"));

        wiremock.verify(exactly(5), getRequestedFor(urlPathEqualTo("/orders/42")));
    }



    @Test
    @DisplayName("Exercise 5: Simulate connection reset by peer Fault")
    void stubConnectionResetByPeerFault(){
        wiremock.stubFor(get(urlPathEqualTo("/orders/fault"))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURI + "/orders/fault"))
                .timeout(Duration.ofMillis(1500))
                .GET()
                .build();

        assertThrows(Exception.class, () ->
                client.send(request, HttpResponse.BodyHandlers.ofString()));
        wiremock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/orders/fault")));
    }


    @Test
    @DisplayName("Exercise 6: Stateful Scenarios with Fault and then Recovered connection")
    void statefulScenariosWithFaultAndRecovery(){
        wiremock.stubFor(get(urlPathEqualTo("/orders/42/recovery"))
                .inScenario("connectionRecoveryScenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"PENDING"}"""))
                .willSetStateTo("BROKEN"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42/recovery"))
                .inScenario("connectionRecoveryScenario")
                .whenScenarioStateIs("BROKEN")
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("RECOVERED"));

        wiremock.stubFor(get(urlPathEqualTo("/orders/42/recovery"))
                .inScenario("connectionRecoveryScenario")
                .whenScenarioStateIs("RECOVERED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 42, "Status":"CONFIRMED"}""")));

//        Case 1: works
        given().when().get("/orders/42/recovery")
                .then().statusCode(202)
                .body("Status", equalTo("PENDING"));

//        Case 2: fails due to fault
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURI + "/orders/42/recovery"))
                .GET()
                .timeout(Duration.ofSeconds(1))
                .build();

        assertThrows(Exception.class, ()->
                client.send(request, HttpResponse.BodyHandlers.ofString()));

//        Case 3: connection recovered
        given().when().get("/orders/42/recovery")
                .then().statusCode(200)
                .body("Status", equalTo("CONFIRMED"));


    }



}






