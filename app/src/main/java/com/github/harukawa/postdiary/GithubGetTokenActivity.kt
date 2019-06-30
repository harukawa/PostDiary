package com.github.harukawa.postdiary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.coroutines.awaitResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class GithubGetTokenActivity : AppCompatActivity() , CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        fun getAppPreferences(ctx : Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        fun getAccessTokenFromPreferences(prefs: SharedPreferences): String {
            return prefs.getString("access_token", "")!!
        }
    }

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    val prefs : SharedPreferences by lazy { getAppPreferences(this) }

    val webView by lazy { findViewById(R.id.webview) as WebView }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_login)

        with(webView.settings) {
            javaScriptEnabled = true
            blockNetworkImage = false
            loadsImagesAutomatically = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if(url?.startsWith(getString(R.string.redirect_uri)) == true) {
                    val code = Uri.parse(url).getQueryParameter("code")
                    code?.let {
                        getAccessToken(code)
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }
        checkValidTokenAndGotoTopIfValid()
    }

    val accessToken: String
        get() = getAccessTokenFromPreferences(prefs)

    val authorizeUrl: String
        get() =
            "https://github.com/login/oauth/authorize?client_id=${getString(R.string.client_id)}" +
                    "&scope=public_repo&redirect_uri=${getString(R.string.redirect_uri)}"


    val apiUrlForCheckTokenValidity : String
        get() {
            return "https://api.github.com/repos/${getString(R.string.user_name)}/${getString(R.string.repo_name)}/contents/_posts/?ref=master"
        }

    fun getAccessToken(code: String) {
        val url =
            "https://github.com/login/oauth/access_token?client_id=${getString(R.string.client_id)}&client_secret=${getString(R.string.client_secret)}&code=$code"

        launch {
            val (_, _, result) = url.httpGet()
                .header("Accept" to "application/json")
                .awaitResponseResult(AuthenticationJson.Deserializer(), Dispatchers.IO)

            val (authjson, _) = result

            authjson?.let {
                prefs.edit()
                    .putString("access_token", it.accessToken)
                    .commit()
            }
            checkTokenValidity(accessToken)
        }
    }

    suspend fun checkTokenValidity(accessToken: String){
        val  (_, response, _) =
            apiUrlForCheckTokenValidity.httpGet()
                .header("Authorization" to "token ${accessToken}")
                .awaitStringResponseResult(scope=Dispatchers.IO)
        if(response.statusCode == 200) {
            val intent: Intent = Intent()
            intent.putExtra("GET_TOKEN", 1)
            setResult(Activity.RESULT_OK,intent)
            finish()
        } else {
            webView.loadUrl(authorizeUrl)
            showMessage("not login ${response.statusCode}")
        }
    }

    fun checkValidTokenAndGotoTopIfValid() {
        val accToken = accessToken
        if (accToken == "") {
            // not valid.
            webView.loadUrl(authorizeUrl)
            return
        }
    }

    data class AuthenticationJson(@SerializedName("access_token") val accessToken : String,
                                  @SerializedName("token_type") val tokenType : String,
                                  val scope: String
    ) {
        class Deserializer : ResponseDeserializable<AuthenticationJson> {
            override fun deserialize(content: String) = Gson().fromJson(content, AuthenticationJson::class.java)
        }
    }



}