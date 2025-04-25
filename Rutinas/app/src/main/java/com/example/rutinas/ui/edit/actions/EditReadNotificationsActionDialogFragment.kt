package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditReadNotificationsActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import com.example.rutinas.util.NotificationReader
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class EditReadNotificationsActionDialogFragment(private val listener: RoutineEditFragment.ActionDialogListener) : BaseEditActionDialogFragment(listener), ActionEditorDialog {
    private var _binding: FragmentEditReadNotificationsActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationReader: NotificationReader // lateinit porque se inicializa mas tarde.

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditReadNotificationsActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditReadNotificationsActionBinding.inflate(layoutInflater)


        if (action == null) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al obtener la acción - No se pudo obtener la acción para editar")
            throw IllegalArgumentException("No se pudo obtener la acción para editar")
        } else {
            try {
                Timber.d("EditReadNotificationsActionDialogFragment: Acción recibida - tipo: ${action?.actionType}, datos: ${action?.data}")
                checkNotificationAccess()
                setupNotificationReader()
                setupUI()
                loadActionData()
            } catch (e: Exception) {
                Timber.e("EditReadNotificationsActionDialogFragment: Error al obtener la acción - ${e.message}")
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Leer Notificaciones")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                saveAction()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun checkNotificationAccess() {
        Timber.d("EditReadNotificationsActionDialogFragment: checkNotificationAccess() llamado")
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationListenerAccessGranted()) {
            Timber.w("EditReadNotificationsActionDialogFragment: No hay acceso a las notificaciones")
            Snackbar.make(
                binding.root,
                "Se requiere acceso a las notificaciones",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Configurar") {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }.show()
        } else {
            Timber.d("EditReadNotificationsActionDialogFragment: Acceso a notificaciones concedido")
        }
    }

    private fun NotificationManager.isNotificationListenerAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context?.contentResolver,
            "enabled_notification_listeners"
        )
        val myNotificationListenerClassName =
            "${requireContext().packageName}/${NotificationReader::class.java.name}"
        return enabledListeners?.contains(requireContext().packageName) == true
    }

    private fun setupNotificationReader(){
        Timber.d("EditReadNotificationsActionDialogFragment: setupNotificationReader() llamado")
        notificationReader = NotificationReader(requireContext())
        notificationReader.initialize { success ->
            if (!success) {
                Timber.e("EditReadNotificationsActionDialogFragment: Error al inicializar el lector de texto")
                Snackbar.make(
                    binding.root,
                    "Error al inicializar el lector de texto",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Timber.d("EditReadNotificationsActionDialogFragment: Lector de texto inicializado correctamente")
            }
        }
    }

    private fun setupUI() {
        Timber.d("EditReadNotificationsActionDialogFragment: setupUI() llamado")
        with(binding) {
            etPackagesToExclude.setText(getDefaultExcludedPackages())

            seekBarDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    Timber.d("EditReadNotificationsActionDialogFragment: Retraso cambiado a $progress segundos")
                    tvDelayValue.text = "$progress segundos"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            btnPreview.setOnClickListener {
                Timber.d("EditReadNotificationsActionDialogFragment: Botón Previsualizar pulsado")
                previewNotificationReading()
            }
        }
    }

    private fun getDefaultExcludedPackages(): String {
        Timber.d("EditReadNotificationsActionDialogFragment: getDefaultExcludedPackages() llamado")
        return listOf(
            "com.spotify.music",
            "com.google.android.youtube",
            "com.android.systemui",
            "com.android.settings"
        ).joinToString(", ")
    }

    private fun loadActionData() {
        Timber.d("EditReadNotificationsActionDialogFragment: loadActionData() llamado")
        if (action != null) {
            try{
                action.data?.let { dataWrapper ->
                    val data = dataWrapper.data
                    data["excludedPackages"]?.let { packages ->
                        Timber.d("EditReadNotificationsActionDialogFragment: Paquetes excluidos cargados: $packages")
                        binding.etPackagesToExclude.setText(packages as String)
                    }

                    data["delay"]?.let { delay ->
                        Timber.d("EditReadNotificationsActionDialogFragment: Retraso cargado: $delay segundos")
                        binding.seekBarDelay.progress = delay as Int
                        binding.tvDelayValue.text = "$delay segundos"
                    }

                }
            }catch(e: Exception){
                Timber.e("EditReadNotificationsActionDialogFragment: Error al cargar datos - ${e.message}")
            }
        }
    }

    private fun previewNotificationReading() {
        Timber.d("EditReadNotificationsActionDialogFragment: previewNotificationReading() llamado")
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationListenerAccessGranted()) {
            Timber.w("EditReadNotificationsActionDialogFragment: No hay acceso a las notificaciones para previsualizar")
            Snackbar.make(
                binding.root,
                "Se requiere acceso a las notificaciones",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val excludedPackages = binding.etPackagesToExclude.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        Timber.d("EditReadNotificationsActionDialogFragment: Paquetes excluidos para previsualización: $excludedPackages")

        notificationManager.activeNotifications?.let { notifications ->
            Timber.d("EditReadNotificationsActionDialogFragment: Leyendo ${notifications.size} notificaciones")
            binding.btnPreview.isEnabled = false
            notificationReader.readNotifications(notifications, excludedPackages) {
                binding.btnPreview.isEnabled = true
                Timber.d("EditReadNotificationsActionDialogFragment: Lectura de notificaciones completada")
            }
        }
    }

    private fun saveAction() {
        Timber.d("EditReadNotificationsActionDialogFragment: saveAction() llamado")
        try {
            val excludedPackages = binding.etPackagesToExclude.text.toString()
            val delay = binding.seekBarDelay.progress

            Timber.d("EditReadNotificationsActionDialogFragment: Guardando - paquetes excluidos: $excludedPackages, retraso: $delay segundos")

            val updatedAction = action.copy(
                data = DataWrapper(mapOf(
                    "excludedPackages" to excludedPackages,
                    "delay" to delay
                ))
            )
            actionUpdateListener?.invoke(updatedAction)
            listener.onActionUpdated(updatedAction)

            Timber.d("EditReadNotificationsActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditReadNotificationsActionDialogFragment: onDestroyView() llamado")
        if(this::notificationReader.isInitialized){
            notificationReader.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action, listener: RoutineEditFragment.ActionDialogListener): EditReadNotificationsActionDialogFragment {
            return EditReadNotificationsActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
