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
import androidx.fragment.app.DialogFragment
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.ArrayList as ArrayList

class MainActivity : AppCompatActivity(),LoadTextDialogFragment.LoadTextDialogListener {

    val preText = """
            |---
            |title: ""
            |date: ${getTextDate()}
            |---
            |
        """.trimMargin()

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    data class mdFile(val fileName:String, val text: String)

    companion object {
        fun getAppPreferences(ctx : Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        fun getPreFileNameFromPreferences(prefs: SharedPreferences): String {
            return prefs.getString("pre_fileName", "")!!
        }

        fun getTextFromPreferences(prefs: SharedPreferences): String {
            return prefs.getString("temporary_text", "")!!
        }
    }
    val prefs : SharedPreferences by lazy {getAppPreferences(this) }

    val pre_fileName : String
        get() = getPreFileNameFromPreferences(prefs)

    val temporaryText : String
        get() = getTextFromPreferences(prefs)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    private val editText: EditText by lazy {
        findViewById<TextView>(R.id.editText) as EditText
    }

    var isPrefile:Boolean = false

    val successSend = 100 // request Code

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_sent -> {
            val fileName = if(isPrefile) pre_fileName else getCurrentTime() + ".md"
            isPrefile = false
            val sentFile :mdFile = mdFile(fileName, editText.text.toString())
            saveFile(sentFile.fileName, sentFile.text)
            prefs.edit().putString("pre_fileName",sentFile.fileName).commit()

            // push text to github
            if(prefs.contains("access_token")) {
                postText(fileName)
            } else {
                val intent = Intent(this, GithubPostActivity::class.java)
                intent.putExtra("FILENAME",sentFile.fileName)
                startActivityForResult(intent, successSend)
            }

            // if success push, editText and title clean
            val success_post = prefs.getInt("success_post", 0)
            if(success_post == 2) {
                showMessage("Success Post")
                editText.setText(preText)
                supportActionBar?.title = getString(R.string.new_title)
            } else {
                showMessage("Faile Post")
            }
            prefs.edit().remove("success_post").commit()
            true
        }
        // temporary save
        R.id.action_save -> {
            prefs.edit().putString("temporary_text", editText.text.toString()).commit()
            showMessage("Save Text")
            true
        }
        R.id.action_edit -> {
            val fileName = pre_fileName
            val text = loadFile(fileName)
            editText.setText(text)
            supportActionBar?.title = fileName
            isPrefile = true
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

    // Fragment.onAttach() in LoadTextDialogFragment callback
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        editText.setText(temporaryText)
        prefs.edit().remove("temporary_text").commit()
    }

    // Fragment.onAttach() in LoadTextDialogFragment callback
    override fun onDialogNegativeClick(dialog: DialogFragment) {
        editText.setText(preText)
        prefs.edit().remove("temporary_text").commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.new_title)
        if(prefs.contains("temporary_text")) {
            val newFragment = LoadTextDialogFragment()
            newFragment.show(supportFragmentManager, "LoadTextDialogFragment")
        } else {
            editText.setText(preText)
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

    fun saveFile(fileName:String, text:String) {
        openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(text.toByteArray())
        }
    }

    fun loadFile(fileName: String): String {
        var data:String = ""
        try {
            val fis = openFileInput(fileName)
            val reader = fis.bufferedReader()
            for (lineBuffer in reader.readLines()) {
                data = data + lineBuffer + "\n"
            }
            reader.close()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return data
    }

    fun postText(fileName: String) {
        try {
            val apiUrl = "https://api.github.com/repos/${getString(R.string.user_name)}/${getString(R.string.repo_name)}/contents/_posts/${fileName}"
            val base64Content = readBase64(fileName)
            val putContent = PutContent(this)
            val accessToken = prefs.getString("access_token","")!!
            putContent.putContentAndFinish(apiUrl, "master", fileName, base64Content, accessToken)

        }catch(e: IllegalArgumentException){
            showMessage("Invalid file. ${e.message}")
        }
    }

    fun readBase64(fileUri : String) : String {
        val inputStream = openFileInput(fileUri)
        try {
            return Base64.encodeToString(inputStream.readBytes(), Base64.DEFAULT)
        } finally {
            inputStream.close()
        }
    }

}
