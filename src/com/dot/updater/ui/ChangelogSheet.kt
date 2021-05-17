package com.dot.updater.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dot.updater.R
import com.dot.updater.changelog.ChangelogAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ChangelogSheet : BottomSheetDialogFragment() {

    private lateinit var recycler: RecyclerView
    private var adapter: ChangelogAdapter? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.changelog_sheet_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.requireViewById(R.id.updateChangelogRecycler)
        recycler.adapter = adapter!!
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val closeButton: MaterialButton = view.requireViewById(R.id.closeSheet)
        closeButton.setOnClickListener { dismiss() }
    }

    fun setupChangelogSheet(adapter: ChangelogAdapter): ChangelogSheet {
        this.adapter = adapter
        return this
    }
}