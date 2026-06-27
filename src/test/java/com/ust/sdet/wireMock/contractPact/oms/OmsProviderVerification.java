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


    @State("Order 123 exists")
    void orderExists() {
        wireMock.stubFor(get(urlEqualTo("/order/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"orderId": 123, "status": "CONFIRMED", "total": 42.0}
                                """)));
    }

    @State("SKU-9 has stock")
    void skuHasStock() {
        wireMock.stubFor(post(urlEqualTo("/orders/84"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 84,
                                  "status": "CONFIRMED",
                                  "total": 42.0
                                }
                                """)));

    }

    @State("Provider can create orders")
    void createOrder() {
        wireMock.stubFor(post(urlEqualTo("/orders"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "statuscode": 201,
                                  "orderId": 125,
                                  "status": "CREATED",
                                  "total": 42.0
                                }
                                """)));
    }

    @State("Order 999 does not exist")
        void orderNotFound() {
            wireMock.stubFor(get(urlEqualTo("/orders/999"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                            {
                              "error": "ORDER_NOT_FOUND",
                              "message": "Order not Found"
                            }
                            """)));
    }

}