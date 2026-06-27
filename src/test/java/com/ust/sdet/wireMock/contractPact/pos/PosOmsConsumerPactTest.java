    package com.ust.sdet.wireMock.contractPact.pos;

    import au.com.dius.pact.consumer.MockServer;
    import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
    import io.restassured.response.Response;
    import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
    import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
    import au.com.dius.pact.consumer.junit5.PactTestFor;
    import au.com.dius.pact.core.model.PactSpecVersion;
    import au.com.dius.pact.core.model.V4Pact;
    import au.com.dius.pact.core.model.annotations.Pact;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;

    import static io.restassured.RestAssured.given;
    import static org.junit.jupiter.api.Assertions.assertEquals;

    @ExtendWith(PactConsumerTestExt.class)
    @PactTestFor(providerName = "oms-provider",pactVersion = PactSpecVersion.V4)
    class PosOmsConsumerPactTest {

        // ---------------- GET ORDER ----------------

        @Pact(provider = "oms-provider", consumer = "pos-consumer")
        V4Pact getOrder(PactDslWithProvider builder) {
            return builder
                    .given("Order 123 exists")
                    .uponReceiving("a request for order 123")
                    .path("/orders/123")
                    .method("GET")
                    .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type",
                            "application/json(;.*)?",
                            "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id", 123)
                            .stringType("status", "CONFIRMED")
                            .numberType("total", 42.0))
                    .toPact(V4Pact.class);
        }


        @Test
        @PactTestFor(pactMethod = "getOrder")
        void testGetOrder(MockServer mockServer) {

            Response response =
                    given()
                            .baseUri(mockServer.getUrl())
                            .when()
                            .get("/orders/123");

            response.then().statusCode(200);
            response.then().log().all();
        }

        // ----------------INVENTORY---------------

        @Pact(provider = "oms-provider", consumer = "pos-consumer")
        V4Pact getInventoryShows(PactDslWithProvider builder){
            return builder
                    .given("SKU-9 has stock")
                    .uponReceiving("a request for Sku-9")
                    .path("/orders/84")
                    .method("GET")
                    .willRespondWith()
                    .status(200)
                    .matchHeader(
                            "Content-Type",
                            "application/json(;.*)?",
                            "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id",84)
                            .stringType("status", "CONFIRMED")
                            .numberType("total", 42.0))
                    .toPact(V4Pact.class);
        }
        @Test
        @PactTestFor(pactMethod = "getInventoryShows")
        void testgetInventory(MockServer mockServer){
            OmsClient client = new OmsClient(mockServer.getUrl());
            OmsClient.Order response = client.getOrder(84);

            assertEquals(84, response.orderId());
            assertEquals("CONFIRMED", response.status());
            assertEquals(42.0, response.total());

        }

        // ---------------- CREATE ORDER ----------------

        @Pact(provider="oms-provider", consumer="pos-consumer")
        V4Pact createOrder(PactDslWithProvider builder){
            return builder
                    .given("Provider can create orders")
                    .uponReceiving("a request to create order")
                    .path("/orders")
                    .method("POST")
                    .matchHeader("Content-Type",
                            "application/json(;.*)?",
                            "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("statuscode", 0)
                            .integerType("orderId", 125)
                            .stringType("status", "NEW")
                            .numberType("total", 42.0))
                    .willRespondWith()
                    .status(201)
                    .matchHeader(
                            "Content-Type",
                            "application/json(;.*)?",
                            "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("statuscode", 201)
                            .integerType("orderId", 125)
                            .stringType("status", "CREATED")
                            .numberType("total", 42.0))
                    .toPact(V4Pact.class);
        }
        @Test
        @PactTestFor(pactMethod = "createOrder")
        void testCreateOrder(MockServer mockServer){
            OmsClient client = new OmsClient(mockServer.getUrl());
            OmsClient.Order request= new OmsClient.Order(
                    0,
                    125,
                    "NEW",
                    42.0);
            OmsClient.Order response = client.createOrder(request);
            assertEquals(125, response.orderId());
            assertEquals("CREATED", response.status());
            assertEquals(42.0, response.total());
        }

        @Pact(provider = "oms-provider", consumer = "pos-consumer")
        V4Pact getOrderNotFoundError(PactDslWithProvider builder){
            return builder
                    .given("Order 999 does not exist")
                    .uponReceiving("a request for non-existent order")
                    .path("/orders/999")
                    .method("GET")
                    .willRespondWith()
                    .status(404)
                    .matchHeader("Content-Type", "application/json(;.*)?",
                            "application/json")
                    .body(new PactDslJsonBody()
                            .stringType("error", "ORDER_NOT_FOUND")
                            .stringType("message", "Order not Found"))
                    .toPact(V4Pact.class);
        }

        @Test
        @PactTestFor(pactMethod = "getOrderNotFoundError")
        void testGetOrderNotFound(MockServer mockServer){
            OmsClient omsclient =  new OmsClient(mockServer.getUrl());
            OmsClient.ErrorMessage  response = omsclient.getNegOrder(999);

            assertEquals("ORDER_NOT_FOUND", response.error());
            assertEquals("Order not Found", response.message());


        }

    }
