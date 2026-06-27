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


    public OmsClient(String baseurl) {
        this.baseUrl=baseurl;
    }


    public Order getOrder(int id) {

        Response response = given()
                .baseUri(baseUrl)
                .basePath("/orders/" + id)
                .when()
                .get();

        response.then().statusCode(200);

        int orderId = response.then().extract().path("id");
        String status = response.then().extract().path("status");
        Number val = response.then().extract().path("total");
        double total = val.doubleValue();


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


    public ErrorMessage getNegOrder(int id) {

        Response response = given()
                .baseUri(baseUrl)
                .when()
                .get("/orders/"+id);

        response.then().statusCode(404);
        String error = response.then().extract().path("error");
        String message = response.then().extract().path("message");


        return new ErrorMessage(error, message);
    }

    record Order(int statuscode,int orderId,String status,double total){}

    record ErrorMessage(String error, String message){}


}
