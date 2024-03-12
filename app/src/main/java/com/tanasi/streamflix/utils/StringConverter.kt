package com.tanasi.streamflix.utils

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class StringConverter : Converter<ResponseBody, String> {

    override fun convert(value: ResponseBody): String {
        return value.bytes().toString(Charsets.UTF_8)
    }
}

class StringConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? = when (type) {
        String::class.java -> StringConverter()
        else -> null
    }

    companion object {
        fun create() = StringConverterFactory()
    }
}