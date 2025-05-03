package com.example.rutinas.ui.edit.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.rutinas.databinding.DialogTriggerTypesBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class TriggerTypeDialog : DialogFragment() {

    interface TriggerTypeListener {
        fun onTriggerSelected(triggerType: TriggerType)
    }

    private var listener: TriggerTypeListener? = null
    private var _binding: DialogTriggerTypesBinding? = null
    private val binding get() = _binding!!

    fun setTriggerTypeListener(listener: TriggerTypeListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTriggerTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnTimeTrigger.setOnClickListener { selectTrigger(TriggerType.TIME) }
        binding.btnCalendarTrigger.setOnClickListener { selectTrigger(TriggerType.CALENDAR) }
        binding.btnLocationTrigger.setOnClickListener { selectTrigger(TriggerType.LOCATION) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.d("TriggerTypeDialog adjuntado al contexto")
    }

    private fun selectTrigger(type: TriggerType) {
        if (isAdded && !isDetached) { // Verificación crítica
            listener?.onTriggerSelected(type)
            dismissAllowingStateLoss() // Dismiss seguro
        } else {
            Timber.e("No se puede seleccionar trigger: Diálogo no adjunto")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class TriggerType {
        TIME, CALENDAR, LOCATION
    }

    companion object {
        fun createInstance() = TriggerTypeDialog()
    }
}
