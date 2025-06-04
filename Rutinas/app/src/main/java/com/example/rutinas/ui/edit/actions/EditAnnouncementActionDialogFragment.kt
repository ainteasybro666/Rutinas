package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels // Import viewModels delegate
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.viewmodel.TextToSpeechViewModel
import dagger.hilt.android.AndroidEntryPoint // Add AndroidEntryPoint import
import timber.log.Timber
import java.util.Locale
// Remove Inject import as TextToSpeech is not injected directly here
// import javax.inject.Inject

@AndroidEntryPoint // Keep AndroidEntryPoint here for ViewModel injection
class EditAnnouncementActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditAnnouncementActionBinding>(listener) {

    // Inject the ViewModel
    private val ttsViewModel: TextToSpeechViewModel by viewModels() // Use viewModels delegate

    // Remove direct TextToSpeech injection
    // @Inject lateinit var textToSpeechLazy: Lazy<TextToSpeech>
    // private val textToSpeech: TextToSpeech get() = textToSpeechLazy.value

    private var availableLanguages: List<Pair<String, String>> = emptyList() // Pair: Language Name, Language Tag
    private var selectedLanguageTag: String? = null

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditAnnouncementActionBinding {
        Timber.d("EditAnnouncementActionDialogFragment: Inflating binding")
        // Use _binding property from the base class
        _binding = FragmentEditAnnouncementActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditAnnouncementActionDialogFragment: onViewCreated() llamado")
        setupUI()
        // Call populateLanguageSpinner from here
        populateLanguageSpinner()

        // loadActionData is called in BaseEditActionDialogFragment.onActivityCreated()
        // loadActionData(actionToEdit) // REMOVE THIS LINE
    }

    private fun setupUI() {
        Timber.d("EditAnnouncementActionDialogFragment: Setting up UI elements other than language spinner.")
        // Setup Volume SeekBar listener (optional, for real-time volume change preview)
        // binding.sbVolume.setOnSeekBarChangeListener(...)

        // Language spinner population is now handled in populateLanguageSpinner() called from onViewCreated
    }

    // Function to populate the language spinner
    private fun populateLanguageSpinner() {
        if (!isAdded || context == null) {
            Timber.w("EditAnnouncementActionDialogFragment: Fragment not added or context is null, cannot populate spinner.")
            return // Avoid issues if fragment is detached
        }

        // Access TextToSpeech via the ViewModel
        val tts = ttsViewModel.tts

        // Check if TextToSpeech has voices before accessing
        try {
            // Accessing tts.voices here
            if (tts.voices == null || tts.voices.isEmpty()) {
                Timber.w("EditAnnouncementActionDialogFragment: TextToSpeech instance has no voices. Cannot populate spinner.")
                binding.spinnerLanguage.isEnabled = false
                showErrorDialog("El motor de texto a voz no tiene voces disponibles.") // Use showErrorDialog from base
                return
            }
        } catch (e: Exception) {
            // Catch potential exceptions during TTS access (less likely with ViewModel but good practice)
            Timber.e(e, "EditAnnouncementActionDialogFragment: Error accessing TextToSpeech instance from ViewModel.")
            binding.spinnerLanguage.isEnabled = false
            showErrorDialog("Error al acceder al motor de texto a voz.") // Use showErrorDialog from base
            return
        }


        try {
            // Use the tts instance obtained from the ViewModel
            availableLanguages = tts.availableLanguages
                .filter { locale -> tts.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE }
                .map { locale ->
                    // Get display name and tag
                    Pair(locale.displayName, locale.toLanguageTag())
                }
                .sortedBy { it.first } // Sort by display name
            Timber.d("EditAnnouncementActionDialogFragment: Found ${availableLanguages.size} available languages.")

            val languageNames = availableLanguages.map { it.first }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languageNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerLanguage.adapter = adapter
            binding.spinnerLanguage.isEnabled = true // Enable spinner once populated

            // Select the previously saved language or default
            loadSavedLanguage()

            binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedLanguageTag = availableLanguages[position].second
                    Timber.d("EditAnnouncementActionDialogFragment: Language selected: ${availableLanguages[position].first} (${selectedLanguageTag})")
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "EditAnnouncementActionDialogFragment: Error populating language spinner.")
            showErrorDialog("Error al cargar la lista de idiomas.") // Use showErrorDialog from base
            binding.spinnerLanguage.isEnabled = false
        }
    }

    private fun loadSavedLanguage() {
        Timber.d("EditAnnouncementActionDialogFragment: Loading saved language.")
        actionToEdit.data?.data?.let { data -> // Use actionToEdit directly
            val savedLanguageTag = data["languageCode"] as? String
            if (!savedLanguageTag.isNullOrBlank()) {
                selectedLanguageTag = savedLanguageTag
                val savedLanguageIndex = availableLanguages.indexOfFirst { it.second == savedLanguageTag }
                if (savedLanguageIndex != -1) {
                    binding.spinnerLanguage.setSelection(savedLanguageIndex)
                    Timber.d("EditAnnouncementActionDialogFragment: Loaded saved language: ${availableLanguages[savedLanguageIndex].first}")
                } else {
                    Timber.w("EditAnnouncementActionDialogFragment: Saved language tag '$savedLanguageTag' not found among available languages. Using default.")
                    showErrorDialog("El idioma guardado no está disponible. Usando el idioma por defecto.") // Use showErrorDialog from base
                    selectDefaultLanguage()
                }
            } else {
                Timber.d("EditAnnouncementActionDialogFragment: No saved language found. Selecting default.")
                selectDefaultLanguage()
            }
        } ?: run {
            Timber.d("EditAnnouncementActionDialogFragment: No action data or language saved. Selecting default.")
            selectDefaultLanguage()
        }
    }

    private fun selectDefaultLanguage() {
        val defaultLocale = Locale.getDefault()
        val defaultLanguageIndex = availableLanguages.indexOfFirst {
            // Try to match by language tag first, then by language code
            it.second == defaultLocale.toLanguageTag() || it.second.startsWith(defaultLocale.language)
        }

        if (defaultLanguageIndex != -1) {
            binding.spinnerLanguage.setSelection(defaultLanguageIndex)
            selectedLanguageTag = availableLanguages[defaultLanguageIndex].second
            Timber.d("EditAnnouncementActionDialogFragment: Selected default language: ${availableLanguages[defaultLanguageIndex].first}")
        } else if (availableLanguages.isNotEmpty()) {
            // Fallback to the first available language if default is not found
            binding.spinnerLanguage.setSelection(0)
            selectedLanguageTag = availableLanguages[0].second
            Timber.w("EditAnnouncementActionDialogFragment: Default language not found among available languages. Falling back to first available: ${availableLanguages[0].first}")
        } else {
            // No languages available at all
            Timber.e("EditAnnouncementActionDialogFragment: No available languages found for selection.")
            showErrorDialog("No hay idiomas de texto a voz disponibles en tu dispositivo.") // Use showErrorDialog from base
            binding.spinnerLanguage.isEnabled = false // Disable spinner
            selectedLanguageTag = null
        }
    }


    // Implementar loadActionData según el método abstracto de la base
    override fun loadActionData(action: Action?) {
        Timber.d("EditAnnouncementActionDialogFragment: loadActionData() llamado")
        // action is already available as actionToEdit in the base class
        actionToEdit.data?.data?.let { data -> // Use actionToEdit directly
            try {
                val message = data["message"] as? String ?: ""
                // Volume is handled by the Seekbar in the layout
                val volume = data["volume"] as? Int ?: 50 // Default volume 50
                binding.etMessage.setText(message)
                binding.sbVolume.progress = volume

                // Language loading is now handled after available languages are fetched in populateLanguageSpinner
                // loadSavedLanguage() // This is called within populateLanguageSpinner after languages are populated

            } catch (e: Exception) {
                Timber.e("EditAnnouncementActionDialogFragment: Error al cargar datos - ${e.message}")
                showErrorDialog("Error al cargar datos de la acción.") // Use showErrorDialog from base
            }
        } ?: Timber.d("EditAnnouncementActionDialogFragment: No action data to load.")
    }

    // Implementar saveActionData según el método abstracto de la base
    override fun saveActionData(): Action {
        Timber.d("EditAnnouncementActionDialogFragment: saveActionData() llamado")
        val message = binding.etMessage.text.toString()
        val volume = binding.sbVolume.progress
        val languageCodeToSave = selectedLanguageTag // Use the selected language tag

        // Basic validation
        if (message.isBlank()) {
            showErrorDialog("El mensaje no puede estar vacío.") // Use showErrorDialog from base
            // Depending on your flow, you might prevent saving here
            // For now, we'll allow saving with empty message but it won't speak
        }
        // Optional: Add validation if language selection is mandatory
        // if (selectedLanguageTag.isNullOrBlank() && availableLanguages.isNotEmpty()) {
        //      showErrorDialog("Por favor, selecciona un idioma.") // Use showErrorDialog from base
        //      // You might return a validation result or throw an exception here
        // }


        // Crear DataWrapper
        val data = DataWrapper(
            mutableMapOf<String, Any>().apply {
                this["message"] = message // Message is required
                this["volume"] = volume // Volume is required
                if (!languageCodeToSave.isNullOrBlank()) {
                    this["languageCode"] = languageCodeToSave // Save selected language code
                } else {
                    // If no language is selected (e.g., no languages available),
                    // we don't save "languageCode". RoutineExecutor will use default.
                }
            }
        )

        // Retornar la Acción actualizada (usar actionToEdit para el UUID, etc.)
        return actionToEdit.copy( // Use actionToEdit from the base class
            actionType = ActionType.ANNOUNCEMENT, // Asegurar el tipo correcto
            data = data
            // routineId, executionOrder, pauseDuration se copian de actionToEdit
        )
    }


    override fun onDestroyView() {
        Timber.d("EditAnnouncementActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        // _binding is nullified in the base class's onDestroyView
        // The injected TextToSpeech instance is managed by the ViewModel.
        // Its shutdown() will be called in TextToSpeechViewModel.onCleared()
        // when the ViewModel is no longer needed.
    }
}
