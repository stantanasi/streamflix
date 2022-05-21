package com.tanasi.retrofit_jsoup.converter

import okhttp3.ResponseBody
import org.jsoup.nodes.Document
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class JsoupConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? = when (type) {
        Document::class.java -> JsoupConverter(retrofit.baseUrl().toString())
        else -> null
    }

    companion object {
        fun create() = JsoupConverterFactory()
    }
}