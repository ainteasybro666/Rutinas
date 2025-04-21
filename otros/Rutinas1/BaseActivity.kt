@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        setupUI()
    }

    abstract fun getViewBinding(): ViewBinding
    abstract fun setupUI()

    // Método para mostrar errores comunes
    protected fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}