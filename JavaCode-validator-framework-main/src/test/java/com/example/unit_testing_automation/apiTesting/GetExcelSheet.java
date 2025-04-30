package com.example.unit_testing_automation.apiTesting;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;

public class GetExcelSheet {
    @BeforeClass
    public void setup(){
        RestAssured.baseURI="http://localhost:8080/unit-test-api/v1";
    }

    @Test
    public void getExcelReport(){
        Response response=
                given()
                        .log() .all()
                        .header("Accept","application/xml")
                        .when()
                        .get("/export-data")
                        .then()
                        .statusCode(200)
                        .log().all()
                        .extract().response();
        System.out.println(response);

    }

    @Test
    public void postExcel(){
        File excelSheet =new File("src/main/java/com/example/unit_testing_automation/TestcaseData/Report1.xlsx");

        Response response=
                given()
                        .log() .all()
                        .headers("Content-Type","multipart/form-data","Accept","application/json")
                        .multiPart("file",excelSheet)
                        .when()
                        .post("/uploadexcel")
                        .then()
                        .statusCode(200)
                        .extract() .response();
    }
}
