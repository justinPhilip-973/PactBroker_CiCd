package com.ust.sdet.wireMock.contractPact.oms;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.*;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@Provider("oms-provider")
@PactBroker(
        url = "http://127.0.0.1:9292"
)

//@PactBroker(
//        url = "https://abcd-a1f2c3d2.pactflow.io",
//        authentication = @PactBrokerAuth(token = "lKudSrGBS_jlJUD3mRpYLg")
//
//)
@PactFolder("target/pacts")
public class OmsProviderVerification {
    @RegisterExtension
    private static final WireMockExtension wireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().port(4010))
                    .build();


    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder
    consumerVersionSelectors() {
        return new SelectorBuilder()
                .mainBranch()
                .deployedOrReleased();
    }


    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("127.0.0.1", 4010));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }


    @State("Order 245 exists")
    void isOrderExists() {
        wireMock.stubFor(get(urlEqualTo("/order/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
    {"orderId": 123, "status": "CONFIRMED", "total": 42.0}
    """)));
    }

    @State("Creating a new order")
    void createOrder() {
        wireMock.stubFor(post(urlEqualTo("/orders/"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
    {"statusCode": 201, "orderId": 101, "status": "CREATED", "total": 2000}
    """)));
    }

    @State("SKU-9 has Stock")
    void getInventory() {
        wireMock.stubFor(get(urlEqualTo("/inventory/SKU-9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
    {"sku": "SKU-9", "qty": 5}
    """)));
    }
}

//
//import au.com.dius.pact.provider.junit5.HttpTestTarget;
//import au.com.dius.pact.provider.junit5.PactVerificationContext;
//import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
//import au.com.dius.pact.provider.junitsupport.Provider;
//import au.com.dius.pact.provider.junitsupport.State;
//import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.TestTemplate;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//
//@Provider("oms-provider")
//@PactBroker(url = "http://localhost:9292")
//public class OmsProviderVerification {
//
//
//    @SuppressWarnings("JUnitMalformedDeclaration")
//    @BeforeEach
//    void setup(PactVerificationContext context){
//        context.setTarget(new HttpTestTarget("localhost", 4010, "/"));
//    }
//
//    @TestTemplate
//    @ExtendWith(PactVerificationInvocationContextProvider.class)
//    void verify(PactVerificationContext context){
//        context.verifyInteraction();
//    }
//
//    @State("Order 123 exists")
//    void isOrderExists(){
//
//    }
//    @State("Sku-9 has stock")
//    void hasStock(){
//
//    }
//    @State("Provider can create orders")
//    void createOrder(){
//
//    }
//}