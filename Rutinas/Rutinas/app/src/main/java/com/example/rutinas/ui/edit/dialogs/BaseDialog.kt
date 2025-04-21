package com.example.rutinas.ui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R

abstract class BaseDialog : DialogFragment() {

    protected abstract fun getLayoutResourceId(): Int
    protected abstract fun setupUI(view: View)
    protected abstract fun onConfirmClicked()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutResourceId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        view.findViewById<Button>(R.id.btnConfirm)?.setOnClickListener {
            onConfirmClicked()
        }
    }
}