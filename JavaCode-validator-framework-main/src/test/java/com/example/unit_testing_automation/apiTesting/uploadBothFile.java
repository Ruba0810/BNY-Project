package com.example.unit_testing_automation.apiTesting;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class uploadBothFile {

    @BeforeClass
    public static void setup(){
        RestAssured.baseURI="http://localhost:8080/unit-test-api/v1";
    }

    @Test
    public void uploadBothFile(){
        File javafile =new File("src/main/java/com/example/unit_testing_automation/TestDataFile/demoTest5.java");
        File excelSheet =new File("src/main/java/com/example/unit_testing_automation/TestcaseData/Report1.xlsx");

        Response responce=
                given()
                        .log() .all()
                        .contentType(ContentType.MULTIPART)
                        .accept(ContentType.TEXT)
                        .multiPart("javaFile",javafile)
                        .multiPart("excelFile",excelSheet)
                        .when()
                        .post("/upload-and-runTest")
                        .then()
                       // .body(equalTo("File uploaded and tested successfully"))
                        .statusCode(200)
                        .extract().response();
    }
    @Test
    public void MissingFile(){


        Response responce=
                given()
                        .log() .all()
                        .accept(ContentType.JSON)
                        .when()
                        .post("/upload-and-runTest")
                        .then()
                        // .body(equalTo("File uploaded and tested successfully"))
                        .statusCode(500)
                        .body("error",equalTo("Internal Server Error"))
                        .extract().response();
    }

    @Test
    public void MissingOneFIle(){

        File javafile =new File("src/main/java/com/example/unit_testing_automation/TestDataFile/demoTest5.java");

        Response responce=
                given()
                        .headers("ContentType","multipart/form-data","Accept","application.text/plain")
                        .multiPart("javaFile",javafile)
                        .log() .all()
                        .accept(ContentType.JSON)
                        .when()
                        .post("/upload-and-runTest")
                        .then()
                        // .body(equalTo("File uploaded and tested successfully"))
                        .statusCode(400)
                        .body("error",equalTo("Bad Request"))
                        .extract().response();
    }
}

