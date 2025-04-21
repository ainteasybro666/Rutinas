class RoutineEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoutineEditBinding
    private val viewModel: RoutineEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddTrigger.setOnClickListener {
            showTriggerTypeDialog()
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveRoutine()
            }
        }
    }

    private fun showTriggerTypeDialog() {
        val items = listOf("Hora específica", "Fecha del calendario", "Ubicación")
        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar activador")
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showTimePicker()
                    // Implementar otros casos
                }
            }.show()
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder().build()
        picker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(picker.hour, picker.minute)
            viewModel.addTrigger(Trigger(type = "TIME", triggerData = time.toString()))
        }
        picker.show(supportFragmentManager, "TIME_PICKER")
    }
}