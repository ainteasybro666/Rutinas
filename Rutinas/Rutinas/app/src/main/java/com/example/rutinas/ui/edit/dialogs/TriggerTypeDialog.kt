package com.example.rutinas.ui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.rutinas.databinding.DialogTriggerTypesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TriggerTypeDialog : DialogFragment() {

    interface TriggerTypeListener {
        fun onTriggerSelected(triggerType: TriggerType)
    }

    private var listener: TriggerTypeListener? = null
    private lateinit var binding: DialogTriggerTypesBinding

    fun setTriggerTypeListener(listener: TriggerTypeListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTriggerTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        with(binding) {
            btnTimeTrigger.setOnClickListener { selectTrigger(TriggerType.TIME) }
            btnCalendarTrigger.setOnClickListener { selectTrigger(TriggerType.CALENDAR) }
            btnLocationTrigger.setOnClickListener { selectTrigger(TriggerType.LOCATION) }
        }
    }

    private fun selectTrigger(type: TriggerType) {
        listener?.onTriggerSelected(type)
        dismiss()
    }

    enum class TriggerType {
        TIME, CALENDAR, LOCATION
    }

    companion object {
        fun newInstance() = TriggerTypeDialog()
    }
}