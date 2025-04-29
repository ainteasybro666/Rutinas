package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogLocationTriggerConfigBinding
import timber.log.Timber

class LocationTriggerDialog : DialogFragment() {
    interface LocationTriggerListener {
        fun onLocationTriggerConfigured(trigger: Trigger)
    }

    private var _binding: DialogLocationTriggerConfigBinding? = null
    private val binding get() = _binding!!
    private var locationTriggerListener: LocationTriggerListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("LocationTriggerDialog: onCreateDialog() llamado")
        _binding = DialogLocationTriggerConfigBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureLocationTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun configureLocationTrigger() {
        Timber.d("LocationTriggerDialog: configureLocationTrigger() llamado")
        val latitude = binding.etLatitude.text.toString().toDoubleOrNull() ?: 0.0
        val longitude = binding.etLongitude.text.toString().toDoubleOrNull() ?: 0.0

        val trigger = Trigger(
            triggerType = "LOCATION",
            data = DataWrapper(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            )
        )
        locationTriggerListener?.onLocationTriggerConfigured(trigger)
    }

    fun setLocationTriggerListener(listener: LocationTriggerListener) {
        this.locationTriggerListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun createInstance(): LocationTriggerDialog {
            return LocationTriggerDialog()
        }
    }
}
