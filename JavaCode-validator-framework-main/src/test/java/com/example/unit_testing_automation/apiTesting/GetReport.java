package com.example.unit_testing_automation.apiTesting;


import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.annotations.*;

import static io.restassured.RestAssured.*;


public class GetReport {


    @BeforeClass
    public static void setup(){
        RestAssured.baseURI="http://localhost:8080/unit-test-api/v1";
    }

@Test
    public void getAllReport() {
        Response response=
                 given()
                               .log() .all()
                        .when()
                        .get("/test-reports")
                        .then()
                        .statusCode(200)
                        .log() .all()
                        .extract().response();
        System.out.println(response.asString());
    }

    @DataProvider(name="getID")
    public Object[][] dataProvider(){
        return new Object[][]{
                {3},
                {4}
        };
    }
    @Test(dataProvider = "getID")
    public void getById(int id){
        Response response=
                (Response) given()
                        .pathParam("id",id)
                        .accept(ContentType.JSON)
                        .log() .all()
                        .when()
                        .get("/test-reports/getById{id}")
                        .then()
                        .statusCode(200)
                        .log() .all()
                        .extract().response();
        System.out.println(response.asString());
    }
    @DataProvider(name="deleteById")
    public Object[][] deleteDataProvide(){
        return new Object[][]{
                {9}
        };
    }
    @Test(dataProvider = "deleteById")
    public void deleteById(int id){
        Response response=
                given()
                        .pathParam("id",id)
                        .log().all()
                        .header("Accept","application/json")
                        .when()
                        .delete("test-reports/deleteById/{id}")
                        .then()
                        .statusCode(200)
                        .extract().response();
        System.out.println(response);

    }
   @Parameters({"userId"})
    @Test
    public void patchById(int userId){
        JSONObject object=new JSONObject();
        object.put("methodName","subtraction");
        Response response=
                 given()
                         .pathParam("id",userId)
                         .header("Content-Type", "application/json")
                         .body(object.toString())
                        .when()
                        .patch("/test-reports/updateById/{id}")
                        .then()
                        .statusCode(200)
                        .log() .all()
                        .extract().response();
    }
    @Test
    public void postData(){
        JSONObject object=new JSONObject();
        object.put("id","55");
        object.put("className","demoTest5");
        object.put("methodName","addition");
        object.put("testStatus","Passed");

        Response response =
                given()
                        .log() .all()
                        .headers("Context-Type","application/json")
                        .when()
                        .post("/test-reports/postdata")
                        .then()
                        .log() .all()
                        .extract() .response();

    }

}
