package com.github.harukawa.postdiary

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView

class ListActivity : AppCompatActivity() {

    val database by lazy { DatabaseHolder(this) }

    var articleDraftsList: MutableList<Column> = mutableListOf()
    var articlePostsList: MutableList<Column> = mutableListOf()
    var articleDrafts: MutableList<String> = mutableListOf()
    var articlePosts: MutableList<String> = mutableListOf()

    val listViewDraft: ListView
            by lazy { findViewById<ListView>(R.id.listDrafts) }
    val listViewPost: ListView
            by lazy { findViewById<ListView>(R.id.listPosts) }

    val adapterPost: ArrayAdapter<String>
            by lazy { ArrayAdapter(this, android.R.layout.simple_list_item_1, articlePosts) }
    val adapterDraft: ArrayAdapter<String>
            by lazy { ArrayAdapter(this, android.R.layout.simple_list_item_1, articleDrafts) }

    val SELECT_FIELDS = arrayOf("_id", "TITLE")

    data class Column(val id: Int, val title: String)

    private fun queryCursor(isPost: Int): Cursor {
        return database.query(DatabaseHolder.ENTRY_TABLE_NAME) {
            select(*SELECT_FIELDS)
            where("IS_POST=?", isPost.toString())
            order("_id ASC")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        articleDraftsList = readData(0)
        articlePostsList = readData(1)

        articleDraftsList.forEach { articleDrafts.add("*" + it.title) }
        articlePostsList.forEach { articlePosts.add(it.title) }

        listViewDraft.adapter = adapterDraft
        listViewPost.adapter = adapterPost

        listViewDraft.setOnItemClickListener() { _, _ /* view */, position, _ /* id */ ->
            createEditActivity(articleDraftsList[position].id, 0)
        }

        listViewPost.setOnItemClickListener() { _, _ /* view */, position, _ /* id */ ->
            createEditActivity(articlePostsList[position].id, 1)
        }
    }

    val editText = 112

    fun createEditActivity(id: Int = -1, isPost: Int = -1) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("EDIT_ID", id)
        intent.putExtra("ISPOST", isPost)
        startActivityForResult(intent, editText)
    }

    fun readData(isPost: Int): MutableList<Column> {
        val cursor = queryCursor(isPost)
        val data: MutableList<Column> = mutableListOf()
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                val title = cursor.getString(cursor.getColumnIndex("TITLE"))
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val col: Column = Column(id, title)
                data.add(col)
                cursor.moveToNext()
            }
        }
        return data
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == editText) {
            articleDrafts.clear()
            articlePosts.clear()
            articleDraftsList = readData(0)
            articlePostsList = readData(1)
            articleDraftsList.forEach { articleDrafts.add("*" + it.title) }
            articlePostsList.forEach { articlePosts.add(it.title) }
            adapterDraft.notifyDataSetChanged()
            adapterPost.notifyDataSetChanged()
            listViewDraft.setSelection(listViewDraft.count - 1)
            listViewPost.setSelection(listViewPost.count - 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_new -> {
            createEditActivity()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}
