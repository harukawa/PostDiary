package com.github.harukawa.postdiary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val database by lazy { DatabaseHolder(this) }

    val preText = """
            |---
            |title: ""
            |date: ${getTextDate()}
            |---
            |
        """.trimMargin()

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    var id : Int = -1
    var prePostFileName : String = ""

    companion object {
        fun getAppPreferences(ctx : Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        fun getPreFileNameFromPreferences(prefs: SharedPreferences): String {
            return prefs.getString("pre_fileName", "")!!
        }
    }
    val prefs : SharedPreferences by lazy {getAppPreferences(this) }

    val pre_fileName : String
        get() = getPreFileNameFromPreferences(prefs)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    private val editText: EditText by lazy {
        findViewById<TextView>(R.id.editText) as EditText
    }

    var isPost : Boolean = false

    val successGetToken = 100 // request Code

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_sent -> {
            val fileName = if(isEdit) pre_fileName else getCurrentTime() + ".md"
            prefs.edit().putString("pre_fileName",fileName).commit()

            if(!prefs.contains("access_token")) {
                val intent = Intent(this, GithubGetTokenActivity::class.java)
                intent.putExtra("FILENAME", fileName)
                startActivityForResult(intent, successGetToken)
            } else {
                // push text to github
                postText(fileName)
            }
            true
        }
        // temporary save
        R.id.action_save -> {
            val file = getCurrentTime() + ".md"
            val title = parserTitle()
            val body = editText.text.toString()
            if(isEdit) {
                if(isPost) {
                    database.updateEntry(id,file,title,body,1)
                } else {
                    database.updateEntry(id, file, title, body,0)
                }
            } else {
                database.insertEntry(file, title, body, 0)
                val saveId = database.getId(file)
                // If the function fail to get the ID, return to the ListActivity
                if(saveId == -1){
                    finish()
                } else {
                    id = saveId
                    isEdit = true
                }
            }
            showMessage("Save Text")
            true
        }
        R.id.action_edit -> {
            if(prefs.contains("pre_fileName")) {
                val (file, body) = database.getEntryFile(pre_fileName)
                if(file == ""){
                    showMessage("No File")
                } else {
                    supportActionBar?.title = file
                    editText.setText(body)
                    showMessage("Load Pre Post Text")
                    isEdit = true
                }
            } else {
                showMessage("No Text")
            }
            true
        }
        R.id.action_setting -> {
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    var isEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val editId = intent.getIntExtra("EDIT_ID", -1)
        id = editId
        val ispost = intent.getIntExtra("ISPOST",-1)
        if(ispost == 1) {
            val (file, body)  = database.getEntry(editId)
            supportActionBar?.title = file
            editText.setText(body)
            isPost = true
            isEdit = true
        } else if(ispost == 0) {
            val (_, body)  = database.getEntry(editId)
            supportActionBar?.title = "新規"
            editText.setText(body)
            isEdit = true
        } else {
            editText.setText(preText)
            supportActionBar?.title = "新規"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == successGetToken) {
            if (resultCode == Activity.RESULT_OK) {
                val sendSuccess = data?.getIntExtra("GET_TOKEN", 0)
                if(sendSuccess == 1) {
                    showMessage("Success Get Token")
                    postText(pre_fileName)
                }
            }
        }
    }

    fun getTextDate() : String {
        val date = Date()
        val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        return format.format(date)
    }
    fun getCurrentTime(): String {
        val date = Date()
        val format = SimpleDateFormat("yyyy-MM-dd-hhmm")
        return format.format(date)
    }

    fun postText(fileName: String) {
        job = Job()
        launch {
            try {
                val settings = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

                val apiUrl =
                    "https://api.github.com/repos/${settings.getString("user_name", "")}/${settings.getString("repo_name", "")}/contents/_posts/$fileName"
                val base64Content = Base64.encodeToString(editText.text.toString().toByteArray(),Base64.DEFAULT)//readBase64(fileName)
                val contentSender = ContentSender()
                val accessToken = prefs.getString("access_token", "")!!
                val successSend = contentSender.putContent(apiUrl, "master", fileName, base64Content, accessToken)

                if(successSend == 1) {
                    if(isEdit) {
                        database.updateEntry(id, fileName, parserTitle(), editText.text.toString(),1)
                        isEdit = false
                    } else {
                        database.insertEntry(fileName, parserTitle(), editText.text.toString(),1)
                    }
                    prePostFileName = fileName
                    editText.setText(preText)
                    supportActionBar?.title = getString(R.string.new_title)
                    showMessage("Success Send")
                } else {
                    showMessage("Fail Send")
                }

            } catch (e: IllegalArgumentException) {
                showMessage("Invalid file. ${e.message}")
            }
        }
    }

    fun parserTitle() :String {
        val text = editText.text.toString()
        val titlePat = "title: \\\"([^\$]+)\\\"".toRegex()

        var res = titlePat.find(text)
        if(res == null) {
            showMessage("No title")
            return "No Title"
        } else {
            val titleLine = res.value
            val title = titleLine.substring(8, titleLine.length - 1)

            return title
        }
    }

}
