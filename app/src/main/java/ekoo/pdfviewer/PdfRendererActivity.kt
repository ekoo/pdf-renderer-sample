package ekoo.pdfviewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ekoo.pdfviewer.databinding.ActivityPdfRendererBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfRendererBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfRendererBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderPdfFromUri(intent.getParcelableExtra(PDF_URI) ?: return)
    }

    private fun renderPdfFromUri(pdfUri: Uri) {

        lifecycleScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                binding.progressBar.isVisible = true
            }

            val pagesUri = mutableListOf<Uri>()

            contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->

                    withContext(Dispatchers.Main) {
                        binding.progressBar.max = renderer.pageCount
                    }

                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val imageUri = savePageIntoCacheDir(page, i)
                            pagesUri.add(imageUri)
                        }

                        withContext(Dispatchers.Main) {
                            binding.progressBar.setProgress(i + 1, true)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                binding.progressBar.isVisible = false
                binding.pdfContentViewPager.adapter = PageAdapter(pagesUri.size, pagesUri)
            }
        }
    }

    private fun savePageIntoCacheDir(page: PdfRenderer.Page, i: Int): Uri {

        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val tempImageFile = File(cacheDir, "page_$i.png")
        val authority = "$packageName.fileprovider"

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        FileOutputStream(tempImageFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        return FileProvider.getUriForFile(this@PdfRendererActivity, authority, tempImageFile)
    }

    inner class PageAdapter(
        private val pageCount: Int,
        private val pagesUri: List<Uri>
    ): FragmentStateAdapter(this) {

        override fun getItemCount() = pageCount

        override fun createFragment(position: Int) = PageFragment.newInstance(pagesUri[position])

    }

    companion object {
        const val PDF_URI = "pdf_uri"

        fun newIntent(context: Context, args: Bundle): Intent {
            return Intent(context, PdfRendererActivity::class.java).apply {
                putExtras(args)
            }
        }
    }
}