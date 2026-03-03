package com.example.geoserver

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiTests {

    @LocalServerPort
    private var port: Int = 8081

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun `test login - successful`() {
        val response: Response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin", "password":"secret123"}""")
            .post("/api/auth/login")

        response.then()
            .statusCode(200)

        val token = response.jsonPath().getString("token")
        assert(token.isNotEmpty())  // Токен должен быть
    }

    @Test
    fun `test login - invalid credentials`() {
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin", "password":"wrong"}""")
            .post("/api/auth/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `test get references contractors - open endpoint`() {
        val response = RestAssured.given()
            .get("/api/references/contractors")
        
        println("GET /api/references/contractors => ${response.statusCode}")
        response.then()
            .statusCode(200)
    }

    @Test
    fun `test get workings - protected endpoint with token`() {
        val loginResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin", "password":"secret123"}""")
            .post("/api/auth/login")

        val token = loginResponse.jsonPath().getString("token")
        println("Login token: $token")

        val response = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .get("/api/workings")
        
        println("GET /api/workings with token => ${response.statusCode}")
        response.then()
            .statusCode(200)
    }

    @Test
    fun `test get workings - protected endpoint without token`() {
        val response = RestAssured.given()
            .get("/api/workings")
        
        println("GET /api/workings without token => ${response.statusCode}")
        response.then()
            .statusCode(401)
    }
}