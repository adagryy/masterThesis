package com.example.grycz.imageprocessor

/**
 * Created by grycz on 3/1/2018.
 */
class MobileMultipartUtility(url: String, charset: String) : MultipartUtility(url, charset) {
    private var responseCode: Int? = null

    fun getResponseCode():Int?{
        return this.responseCode
    }

    fun mobileFinish() {
        writer.append(LINE_FEED).flush()
        writer.append("--$boundary--").append(LINE_FEED)
        writer.close()

        // checks server's status code first
        responseCode = httpsConn.responseCode
    }
}

