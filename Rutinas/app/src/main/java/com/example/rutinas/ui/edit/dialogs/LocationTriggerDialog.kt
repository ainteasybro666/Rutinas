package com.example.rutinas.ui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogLocationTriggerBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class LocationTriggerDialog : DialogFragment() {

    interface LocationTriggerListener {
        fun onLocationTriggerConfigured(trigger: Trigger)
    }

    private var listener: LocationTriggerListener? = null
    private lateinit var binding: DialogLocationTriggerBinding

    fun setLocationTriggerListener(listener: LocationTriggerListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLocationTriggerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.btnConfirm.setOnClickListener {
            val latitudeText = binding.etLatitude.text.toString()
            val longitudeText = binding.etLongitude.text.toString()
            val latitude = latitudeText.toDoubleOrNull() ?: 0.0
            val longitude = longitudeText.toDoubleOrNull() ?: 0.0

            val configData = mapOf("latitude" to latitude, "longitude" to longitude)
            val trigger = Trigger(
                uuid = UUID.randomUUID().toString(),
                routineId = 0, // routineId se asigna al guardar la rutina
                triggerType = "LOCATION",
                data = configData
            )
            listener?.onLocationTriggerConfigured(trigger)
            dismiss()
        }
    }

    companion object {
        fun createInstance() = LocationTriggerDialog()
    }
}