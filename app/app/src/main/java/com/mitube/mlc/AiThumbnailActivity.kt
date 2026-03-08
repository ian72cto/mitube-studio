package com.mitube.mlc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AiThumbnailActivity : AppCompatActivity() {

    private lateinit var btnGenerate: Button
    private lateinit var etPrompt: EditText
    private lateinit var llImageContainer: LinearLayout
    private lateinit var rvGeneratedImages: RecyclerView
    
    // Selected images for reference
    private val selectedImageUris = mutableListOf<Uri>()
    private var generatedBitmaps = listOf<Bitmap>()
    private var selectedGeneratedImageIndex = -1

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
            updateImagePreviewContainer()
        } else {
            Toast.makeText(this, "선택된 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_thumbnail)

        btnGenerate = findViewById(R.id.btnGenerate)
        etPrompt = findViewById(R.id.etPrompt)
        llImageContainer = findViewById(R.id.llImageContainer)
        rvGeneratedImages = findViewById(R.id.rvGeneratedImages)

        rvGeneratedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<android.widget.FrameLayout>(R.id.btnAddImages).setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnGenerate.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            if (prompt.isEmpty() && selectedImageUris.isEmpty()) {
                Toast.makeText(this, "프롬프트나 이미지를 하나 이상 넣어주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedGeneratedImageIndex != -1 && generatedBitmaps.isNotEmpty()) {
                // Return the generated image to LiveReservationActivity
                saveAndReturnImage(generatedBitmaps[selectedGeneratedImageIndex])
            } else {
                // Generate new images
                generateMockAiThumbnails(prompt)
            }
        }
    }

    private fun updateImagePreviewContainer() {
        // Keep the add button, remove previously added previews
        val addButtonIndex = 0
        val viewCount = llImageContainer.childCount
        if (viewCount > 1) {
            llImageContainer.removeViews(1, viewCount - 1)
        }

        val size = (100 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()

        for (uri in selectedImageUris) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 0, margin, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
            llImageContainer.addView(imageView)
        }
    }

    private fun generateMockAiThumbnails(prompt: String) {
        btnGenerate.isEnabled = false
        btnGenerate.text = "AI가 썸네일을 그리고 있습니다..."
        
        CoroutineScope(Dispatchers.Main).launch {
            // Simulate network delay
            withContext(Dispatchers.IO) { delay(2500) }
            
            // Generate dummy bitmaps
            generatedBitmaps = listOf(
                createDummyBitmap(Color.parseColor("#FFCDD2"), prompt),
                createDummyBitmap(Color.parseColor("#E1BEE7"), prompt),
                createDummyBitmap(Color.parseColor("#BBDEFB"), prompt),
                createDummyBitmap(Color.parseColor("#C8E6C9"), prompt)
            )

            val adapter = GeneratedThumbnailAdapter(generatedBitmaps) { selectedIndex ->
                selectedGeneratedImageIndex = selectedIndex
                btnGenerate.text = "이 썸네일로 최종 결정하기"
            }
            rvGeneratedImages.adapter = adapter
            
            btnGenerate.isEnabled = true
            Toast.makeText(this@AiThumbnailActivity, "생성 완료! 마음에 드는 썸네일을 선택하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDummyBitmap(color: Int, prompt: String): Bitmap {
        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(color)
        
        val paint = android.graphics.Paint().apply {
            this.color = Color.BLACK
            textSize = 60f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val text = if (prompt.isNotEmpty()) prompt else "Generated AI Thumbnail"
        canvas.drawText(text, 640f, 360f, paint)
        
        return bitmap
    }

    private fun saveAndReturnImage(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.Main).launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val file = File(cacheDir, "ai_thumbnail_${System.currentTimeMillis()}.jpg")
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                    out.close()
                    Uri.fromFile(file)
                } catch (e: Exception) {
                    null
                }
            }

            if (uri != null) {
                val intent = Intent().apply {
                    putExtra("EXTRA_THUMBNAIL_URI", uri.toString())
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this@AiThumbnailActivity, "저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
