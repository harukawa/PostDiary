package com.github.harukawa.postdiary

import android.app.Activity
import android.content.Intent
import android.graphics.PostProcessor
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView

class ListActivity : AppCompatActivity() {

    var fileDrafts: MutableList<String> = mutableListOf()
    var filePosts: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        var fileDraftsList = fileList()

        fileDraftsList.forEach {  fileDrafts.add(it) }
        fileDraftsList.forEach {  filePosts.add(it) }

        val listViewDraft: ListView = findViewById<ListView>(R.id.listDrafts)
        val listViewPost: ListView = findViewById<ListView>(R.id.listPosts)

        val adapterDrafts: ArrayAdapter<String> = ArrayAdapter(this,android.R.layout.simple_list_item_1, fileDrafts)
        val adapterPosts: ArrayAdapter<String> = ArrayAdapter(this,android.R.layout.simple_list_item_1, filePosts)

        listViewDraft.adapter = adapterDrafts
        listViewPost.adapter = adapterPosts

        val a = listViewDraft

        listViewDraft.setOnItemClickListener() { _, _ /* view */, position, _ /* id */ ->
            createEditActivity(fileDrafts[position].toString())
        }

        listViewPost.setOnItemClickListener() { _, _ /* view */, position, _ /* id */ ->
            createEditActivity(filePosts[position].toString())
        }
    }

    val editText = 1

    fun createEditActivity(fileName : String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("EDIT_FILENAME", fileName)
        startActivityForResult(intent,editText)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == editText) {
            var fileDraftsList = fileList()
            fileDraftsList.forEach {  fileDrafts.add(it) }
            fileDraftsList.forEach {  filePosts.add(it) }

            val listViewDraft: ListView = findViewById<ListView>(R.id.listDrafts)
            val listViewPost: ListView = findViewById<ListView>(R.id.listPosts)

            val adapterDrafts: ArrayAdapter<String> = ArrayAdapter(this,android.R.layout.simple_list_item_1, fileDrafts)
            val adapterPosts: ArrayAdapter<String> = ArrayAdapter(this,android.R.layout.simple_list_item_1, filePosts)

            listViewDraft.adapter = adapterDrafts
            listViewPost.adapter = adapterPosts
        }
    }
}
