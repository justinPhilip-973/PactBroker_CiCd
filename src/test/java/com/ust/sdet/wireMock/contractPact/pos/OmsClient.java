package com.ust.sdet.wireMock.contractPact.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;
import java.util.function.BooleanSupplier;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OmsClient {


    private String baseUrl;
    private HttpClient client;

    private static final String BASE_URL = System.getProperty(
            "baseUrl",
            System.getenv().getOrDefault("BASE_URL", "http://localhost:4010/")
    );

    public OmsClient(String baseurl) {
        this.baseUrl=baseurl;
    }

    @BeforeEach
    void setup() {
        this.baseUrl = BASE_URL;
        this.client = HttpClient.newHttpClient();
    }

    public Order getOrder(int id) {

        Response response = given()
                .baseUri(baseUrl)
                .basePath("/orders/" + id)
                .get();

        response.then().statusCode(200);

        int orderId = response.then().extract().path("id");
        String status = response.then().extract().path("status");
        int total = response.then().extract().path("total");

        return new Order(response.statusCode(),orderId,status,total);
    }

    public Order createOrder(Order order){
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(order)
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .extract()
                .as(Order.class);
    }


//    public ErrorMessage getNegOrder(int id) {
//
////        Response response = given()
////                .baseUri(baseUrl)
////                .basePath("/orders/" + id)
////                .get();
////
////        response.then().statusCode(200);
//        Response response = given()
//                .baseUri(baseUrl)
//                .when()
//                .get("/orders/"+id);
//
////        response.then().statusCode(404);
//
//        String error = response.then().extract().path("error");
//        int orderId = response.then().extract().path("id");
//        String status = response.then().extract().path("status");
//        double total = response.then().extract().path("total");
//
//        return new Order(response.statusCode(),orderId,status,total);
//    }

    record Order(int statuscode,int orderId,String status,double total){}

    record ErrorMessage(String error, String message){}


}
