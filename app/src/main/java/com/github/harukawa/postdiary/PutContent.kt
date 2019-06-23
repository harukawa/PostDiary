package com.github.harukawa.postdiary


import android.content.Context
import android.content.SharedPreferences
import com.github.harukawa.postdiary.MainActivity.Companion.getAppPreferences
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.coroutines.awaitResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.*
import java.io.StringWriter
import kotlin.coroutines.CoroutineContext

class PutContent(context: Context): CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val prefs : SharedPreferences by lazy { getAppPreferences(context) }

    data class Content(val content : String, val sha : String) {
        class Deserializer : ResponseDeserializable<Content> {
            override fun deserialize(content: String) = Gson().fromJson(content, Content::class.java)
        }
    }

    fun arrayListOfContentParameter(branchName: String, fname: String, base64Content: String) = arrayListOf(
        "path" to fname,
        "message" to "Put from PostGitHubpages",
        "content" to base64Content,
        "branch" to branchName
    )

    fun jsonBuilder(builder: JsonWriter.()->Unit) : String {
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.builder()
        return sw.toString()
    }

    fun putContentAndFinish(apiUrl: String, branchName: String, fname: String, base64Content: String, accessToken: String) {
        job = Job()
        launch {
            val resp = putContent(apiUrl, branchName, fname, base64Content, accessToken)

            val sendCode: Int = when(resp.statusCode) {
                200, 201 -> 2
                else -> 1
            }
            prefs.edit().putInt("success_post",sendCode).commit()
        }
    }

    suspend fun putContent(apiUrl: String, branchName: String, fname: String, base64Content: String, accessToken:String) : Response {
        val (_, _, result) = "$apiUrl?ref=$branchName".httpGet()
            .header("Authorization" to "token ${accessToken}")
            .awaitResponseResult(Content.Deserializer(), Dispatchers.IO)

        val contParam = arrayListOfContentParameter(branchName, fname, base64Content)

        result.fold(
            { cont -> contParam.add("sha" to cont.sha) },
            { _ /* err */ -> {} }
        )


        val json = jsonBuilder {
            val obj = beginObject()
            contParam.map { (k, v) -> obj.name(k).value(v) }
        }

        val (_, resp, _) = apiUrl.httpPut()
            .body(json)
            .header("Authorization" to "token ${accessToken}")
            .header("Content-Type" to "application/json")
            .awaitStringResponseResult(scope = Dispatchers.IO)
        return resp
    }
}