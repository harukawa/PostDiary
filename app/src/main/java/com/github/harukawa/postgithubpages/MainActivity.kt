package com.github.harukawa.postgithubpages

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.ArrayList as ArrayList

class MainActivity : AppCompatActivity() {

    data class mdFile(val fileName:String, val text: String)

    companion object {
        fun getAppPreferences(ctx : Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        fun getPreFileNameFromPreferences(prefs: SharedPreferences): String {
            return prefs.getString("pre_fileName", "")
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

    var isPrefile:Boolean = false

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_sent -> {
            var fileName = ""
            if(isPrefile) {
                fileName = pre_fileName
            } else {
                fileName = getCurrentTime() + ".md"
            }
            isPrefile = false
            val sentFile :mdFile = mdFile(fileName, editText.text.toString())
            saveFile(sentFile.fileName, sentFile.text)
            prefs.edit().putString("pre_fileName",sentFile.fileName).commit()

            val intent = Intent(this, GithubPostBaseActivity::class.java)
            intent.putExtra("FILENAME",sentFile.fileName)
            startActivity(intent)
            true
        }
        R.id.action_edit -> {
            val fileName = pre_fileName
            val text = loadFile(fileName)
            editText.setText(text)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val preText = """
            |---
            |title: ""
            |date: ${getTextDate()}
            |---
            |
        """.trimMargin()
        editText.setText(preText)
    }

    fun getTextDate() : String {
        val date = Date()
        val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        return format.format(date)
    }
    fun getCurrentTime(): String {
        val date = Date()
        val format = SimpleDateFormat("yyyy-MM-ddhhmm")
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

}
