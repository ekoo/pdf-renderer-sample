package ekoo.pdfviewer

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.google.android.material.color.DynamicColors
import ekoo.pdfviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        val uri = it ?: return@registerForActivityResult
        val args = bundleOf(PdfRendererActivity.PDF_URI to uri)
        startActivity(PdfRendererActivity.newIntent(this, args))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(application)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickPdfFileButton.setOnClickListener {
            pickPdfLauncher.launch("application/pdf")
        }
    }
}