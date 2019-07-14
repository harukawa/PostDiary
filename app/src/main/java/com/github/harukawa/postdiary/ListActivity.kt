package com.github.harukawa.postdiary

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class ListActivity : AppCompatActivity() {

    val database by lazy { DatabaseHolder(this) }

    var articleDraftsList: MutableList<Article> = mutableListOf()
    var articlePostsList: MutableList<Article> = mutableListOf()

    val listViewDraft: ListView
            by lazy { findViewById<ListView>(R.id.listDrafts) }
    val listViewPost: ListView
            by lazy { findViewById<ListView>(R.id.listPosts) }

    val adapterPost: ArrayAdapter<Article>
            by lazy {articleListAdapter(this,0,articlePostsList)}
    val adapterDraft: ArrayAdapter<Article>
            by lazy {articleListAdapter(this, 0,articleDraftsList)}

    val SELECT_FIELDS = arrayOf("_id", "TITLE")

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

        articleDraftsList.addAll(readData(0))
        articlePostsList.addAll(readData(1))

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

    fun readData(isPost: Int): MutableList<Article> {
        val cursor = queryCursor(isPost)
        val data: MutableList<Article> = mutableListOf()
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                val title = cursor.getString(cursor.getColumnIndex("TITLE"))
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val col: Article = Article(id, title)
                data.add(col)
                cursor.moveToNext()
            }
        }
        if(isPost==0){
            data.let{ for(i in it) i.addAsterisk()}
        }
        return data
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == editText) {
            articleDraftsList.clear()
            articlePostsList.clear()
            articleDraftsList.addAll(readData(0))
            articlePostsList.addAll(readData(1))
            adapterDraft.notifyDataSetChanged()
            adapterPost.notifyDataSetChanged()
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

data class ViewHolder(val title: TextView, val deleteButton: ImageButton)

class articleListAdapter : ArrayAdapter<Article> {

    constructor(context : Context, resource : Int, objects: MutableList<Article>) : super(context,resource,objects) {}

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    val database by lazy { DatabaseHolder(context) }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.list_items, parent, false)
        val viewHolder : ViewHolder = if(convertView == null)  ViewHolder(view.findViewById(R.id.textView), view.findViewById(R.id.deleteImageButton))
                else view.tag  as ViewHolder
        if(convertView == null) {
            view.tag = viewHolder
        }

        val Item = getItem(position)
        viewHolder.title.text = Item!!.title
        viewHolder.deleteButton.setOnClickListener { _ ->
            // touch deleteButton
            val dialog = AlertDialog.Builder(context)
            dialog.setMessage(R.string.delete_message)
                .setPositiveButton(R.string.yes,
                    { _, _ ->
                        database.deleteEntries(Item.id)
                        this.remove(Item)
                        this.notifyDataSetChanged()
                    })
                .setNegativeButton(R.string.no,
                    { _, _ ->
                        // User cancelled the dialog
                    })
            // Create the AlertDialog object and return it
            dialog.create()
            dialog.show()
        }
        return view!!
    }
}

class Article(val id: Int, var title: String) {
    fun addAsterisk() {
        this.title = "*" + this.title
    }
}
