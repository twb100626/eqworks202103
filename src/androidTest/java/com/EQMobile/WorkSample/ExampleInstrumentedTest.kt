package com.EQMobile.WorkSample

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

import org.junit.Test
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private var mockWebServer : MockWebServer? = null

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer!!.enqueue(
                MockResponse().apply {
                    setResponseCode(200)
                    setBody("success")
                }
        )
    }

    @After
    fun dropdown() {
        mockWebServer?.shutdown()
    }

    @Test
    fun testLog() {
        //    {
        //      "args": {},
        //      "data": "",
        //      "files": {},
        //      "form": {
        //        "ext": "ext",
        //        "lat": "1.0",
        //        "lon": "1.0",
        //        "time": "1615286801572"
        //      },
        //      "headers": {
        //        "Accept-Encoding": "gzip",
        //        "Content-Length": "42",
        //        "Content-Type": "application/x-www-form-urlencoded",
        //        "Host": "httpbin.org",
        //        "User-Agent": "okhttp/4.7.2",
        //        "X-Amzn-Trace-Id": "Root=1-60475058-2bd6b20710ae13af4bcdc78a"
        //      },
        //      "json": null,
        //      "origin": "27.246.255.1",
        //      "url": "https://httpbin.org/post"
        //    }
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val classUnderTest = Library(appContext)
        classUnderTest.setUrl("https://httpbin.org/post")
        var time : Long = System.currentTimeMillis() / 1000L
        classUnderTest.log(LocationEvent(11f, 12f, time, "empty"))

        var iCount : Int = 0
        var response : String? = null
        do {
            println("Wait for 1 second")
            Thread.sleep(1000)
            response = classUnderTest.getResponse()
            if (response != null) break
            iCount++
        } while (iCount < 330)

        assertNotNull(response)
        var parser = JSONParser()
        var json : JSONObject = parser.parse(response) as JSONObject
        assertTrue (json.contains("form"))
        json = json.get("form") as JSONObject
        assertTrue (json.contains("lat"))
        assertTrue (json.contains("lon"))
        assertTrue (json.contains("time"))
        assertTrue (json.contains("ext"))
        assertEquals("11.0", json.get("lat"))
        assertEquals("12.0", json.get("lon"))
        assertEquals(time.toString(), json.get("time"))
        assertEquals("empty", json.get("ext"))
    }

    @Test
    fun testLog2() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val classUnderTest = Library(appContext)
        classUnderTest.setUrl("https://httpbin.org/post")
        var time : Long = System.currentTimeMillis() / 1000L
        classUnderTest.log(LocationEvent(-91f, -182f, 0L, "empty"))

        var iCount : Int = 0
        var response : String? = null
        do {
            println("Wait for 1 second")
            Thread.sleep(1000)
            response = classUnderTest.getResponse()
            if (response != null) break
            iCount++
        } while (iCount < 330)

        assertNotNull(response)
        var parser = JSONParser()
        var json : JSONObject = parser.parse(response) as JSONObject
        assertTrue (json.contains("form"))
        json = json.get("form") as JSONObject
        assertTrue (json.contains("lat"))
        assertTrue (json.contains("lon"))
        assertTrue (json.contains("time"))
        assertTrue (json.contains("ext"))
        assertEquals("0.0", json.get("lat"))
        assertEquals("0.0", json.get("lon"))
        assertNotEquals(time.toString(), "0")
        assertEquals("empty", json.get("ext"))
    }

    @Test
    fun testLog_mock_server() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val classUnderTest = Library(appContext)
        classUnderTest.setHttpUrl(mockWebServer!!.url("/"))
        var time : Long = System.currentTimeMillis() / 1000L
        classUnderTest.log(LocationEvent(0f, 0f, time, "empty"))

        var iCount = 0
        var response : String?
        do {
            println("Wait for 1 second")
            Thread.sleep(1000)
            response = classUnderTest.getResponse()
            if (response != null) break
            iCount++
        } while (iCount < 10)

        assertNotNull(response)
        assertEquals("success", response)
    }
}
