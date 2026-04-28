package com.example.geoserver

import io.restassured.RestAssured
import io.restassured.response.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ReportTemplateUploadIT {
    // private val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc3NzA4NTA3NCwiZXhwIjoxNzc3Njg5ODc0fQ.dewVwaV8CWIkiYHcQofY2lOOrRFgtSkL2jrulvnM2X4"
    // private val filePath = "C:/Users/IIIYTNIK/Desktop/Test_first.jrxml"
    // private val url = "http://localhost:8081/api/report-templates/upload"

    // @Test
    // fun uploadJrxmlTemplate() {
    //     val file = getOrCreateJrxmlFile()

    //     val response: Response = RestAssured.given()
    //         .header("Authorization", "Bearer $token")
    //         .multiPart("file", file)
    //         .multiPart("name", "Test template")
    //         .multiPart("description", "Test upload via RestAssured")
    //         .`when`()
    //         .post(url)
    //         .then()
    //         .extract().response()

    //     println("Status: ${response.statusCode}")
    //     println("Body: ${response.body.asString()}")

    //     assertEquals(200, response.statusCode, "Expected HTTP 200 OK")
    // }

    // private fun getOrCreateJrxmlFile(): File {
    //     val file = File(filePath)
    //     if (file.exists()) return file

    //     val jrxmlContent = """
    //         <?xml version=\"1.0\" encoding=\"UTF-8\"?>
    //         <jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\"
    //             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
    //             xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports
    //             http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\"
    //             name=\"TestReport\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" uuid=\"12345678-1234-1234-1234-123456789012\">
    //             <detail>
    //                 <band height=\"20\">
    //                     <staticText>
    //                         <reportElement x=\"0\" y=\"0\" width=\"200\" height=\"20\"/>
    //                         <text><![CDATA[Hello, Jasper!]]></text>
    //                     </staticText>
    //                 </band>
    //             </detail>
    //         </jasperReport>
    //     """.trimIndent()
    //     Files.write(file.toPath(), jrxmlContent.toByteArray())
    //     return file
    // }
}
