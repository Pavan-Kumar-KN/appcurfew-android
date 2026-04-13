package com.pavan.appcurfew

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var prefs: BedtimePrefs
    private lateinit var adapter: AppListAdapter
    private val apps = mutableListOf<SelectableApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        prefs = BedtimePrefs(this)
        val listView = findViewById<ListView>(R.id.listApps)
        val saveButton = findViewById<Button>(R.id.buttonSaveBlockedApps)

        title = getString(R.string.select_apps_title)

        apps.addAll(loadLaunchableApps())
        adapter = AppListAdapter(apps)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val selectedPackages = prefs.getBlockedPackages()
        apps.forEachIndexed { index, app ->
            if (app.packageName in selectedPackages) {
                listView.setItemChecked(index, true)
                app.checked = true
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            apps[position].checked = !apps[position].checked
            adapter.notifyDataSetChanged()
        }

        saveButton.setOnClickListener {
            val blocked = apps.filter { it.checked }.mapTo(mutableSetOf()) { it.packageName }
            prefs.setBlockedPackages(blocked)
            Toast.makeText(this, R.string.selected_apps_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadLaunchableApps(): List<SelectableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .map {
                val appInfo = it.activityInfo.applicationInfo
                SelectableApp(
                    packageName = it.activityInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private data class SelectableApp(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        var checked: Boolean = false
    )

    private class AppListAdapter(
        private val items: List<SelectableApp>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.row_app_item, parent, false)
            val icon = view.findViewById<ImageView>(R.id.imageAppIcon)
            val title = view.findViewById<TextView>(R.id.textAppName)
            val packageText = view.findViewById<TextView>(R.id.textAppPackage)
            val checkbox = view.findViewById<CheckBox>(R.id.checkboxApp)

            val item = items[position]
            icon.setImageDrawable(item.icon)
            title.text = item.label
            packageText.text = item.packageName
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.checked
            checkbox.setOnCheckedChangeListener { _, isChecked -> item.checked = isChecked }

            view.setOnClickListener {
                item.checked = !item.checked
                notifyDataSetChanged()
            }

            return view
        }
    }
}