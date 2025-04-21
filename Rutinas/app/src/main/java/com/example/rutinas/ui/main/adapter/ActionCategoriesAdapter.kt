package com.example.rutinas.ui.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.example.rutinas.R

class ActionCategoriesAdapter(
    private val context: Context,
    private val categoryList: List<String>,
    private val actionMap: HashMap<String, List<String>>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = categoryList.size

    override fun getChildrenCount(groupPosition: Int): Int {
        val category = categoryList[groupPosition]
        return actionMap[category]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any = categoryList[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val category = categoryList[groupPosition]
        return actionMap[category]?.get(childPosition) ?: ""
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_group_action_category, parent, false)
        val textView = view.findViewById<TextView>(R.id.tvCategoryName)
        textView.text = getGroup(groupPosition) as String
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_action, parent, false)
        val textView = view.findViewById<TextView>(R.id.tvActionName)
        textView.text = getChild(groupPosition, childPosition) as String
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}
