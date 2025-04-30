package com.example.unit_testing_automation.apiTesting;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


public class FileUploadTest {

    @BeforeClass
    public static void setup() {

        RestAssured.baseURI = "http://localhost:8080/unit-test-api/v1";
        RestAssured.registerParser("text/plain", Parser.TEXT);
    }

    @Test
    public void testFileUpload() {
        System.out.println("Starting testFileUpload...");
        File file = new File("src/main/java/com/example/unit_testing_automation/TestDataFile/demoTest5.java");
        Response response =
                given()
                      .multiPart("file", file)
                      .log().all()
                .when()
                        .post("/upload-file")
                .then()
                        .statusCode(200)
                        .body(equalTo("File uploaded and tested successfully"))
                        .log().all()
                        .extract()
                        .response();

        System.out.println(response.toString());
    }

    @Test
    public void testFileUploadMissing() {

                given()
                        .multiPart("file", "") // wrong key
                        .log().all()
                .when()
                        .post("/upload-file")
                .then()
                        .statusCode(400) // more appropriate than 500
                        //.body("error", equalTo("Bad Request"))
                        .log().all();

    }
}
