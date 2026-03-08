package com.mitube.mlc

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mitube.mlc.api.ImagenApiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AiThumbnailActivity : AppCompatActivity() {

    private lateinit var btnGenerate: Button
    private lateinit var etPrompt: EditText
    private lateinit var llImageContainer: LinearLayout
    private lateinit var rvGeneratedImages: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var layoutResult: View

    private val selectedImageUris = mutableListOf<Uri>()
    private var generatedBitmaps = listOf<Bitmap>()
    private var selectedGeneratedImageIndex = -1

    private val imagenManager = ImagenApiManager()

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                selectedImageUris.clear()
                selectedImageUris.addAll(uris)
                updateImagePreviewContainer()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_thumbnail)

        btnGenerate = findViewById(R.id.btnGenerate)
        etPrompt = findViewById(R.id.etPrompt)
        llImageContainer = findViewById(R.id.llImageContainer)
        rvGeneratedImages = findViewById(R.id.rvGeneratedImages)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        layoutResult = findViewById(R.id.layoutResult)

        rvGeneratedImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 뒤로가기 버튼
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // 레퍼런스 이미지 추가 버튼
        findViewById<View>(R.id.btnAddImages).setOnClickListener {
            pickMultipleMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // 생성 / 확정 버튼
        btnGenerate.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            if (prompt.isEmpty() && selectedImageUris.isEmpty()) {
                Toast.makeText(this, "프롬프트나 레퍼런스 이미지를 하나 이상 넣어주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedGeneratedImageIndex != -1 && generatedBitmaps.isNotEmpty()) {
                // 썸네일 선택 확정 → 이전 화면으로 반환
                saveAndReturnImage(generatedBitmaps[selectedGeneratedImageIndex])
            } else {
                // API 호출로 이미지 생성
                generateImages(prompt)
            }
        }
    }

    // ───────────────────────────────────────────────
    // 이미지 생성 (Pollinations.ai - API 키 불필요)
    // ───────────────────────────────────────────────

    private fun generateImages(prompt: String) {
        btnGenerate.isEnabled = false
        btnGenerate.text = "생성 중..."
        progressBar.visibility = View.VISIBLE
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "AI가 썸네일을 그리고 있습니다... (최대 2분 소요)"
        layoutResult.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            val result = imagenManager.generateImages(
                prompt = buildEnhancedPrompt(prompt),
                sampleCount = 4
            )

            progressBar.visibility = View.GONE
            tvStatus.visibility = View.GONE

            result.fold(
                onSuccess = { bitmaps ->
                    if (bitmaps.isEmpty()) {
                        showError("이미지 생성 결과가 없습니다. 다른 프롬프트로 다시 시도해 주세요.")
                    } else {
                        generatedBitmaps = bitmaps
                        showGeneratedImages(bitmaps)
                    }
                },
                onFailure = { error ->
                    showError(error.message ?: "알 수 없는 오류가 발생했습니다.")
                }
            )

            btnGenerate.isEnabled = true
        }
    }

    /**
     * 레퍼런스 이미지가 있을 경우 참조 스타일을 프롬프트에 반영
     */
    private fun buildEnhancedPrompt(userPrompt: String): String {
        val styleHint = if (selectedImageUris.isNotEmpty()) {
            "inspired by the style and composition of the reference images"
        } else {
            "cinematic, vibrant, high contrast"
        }
        return "$userPrompt, $styleHint"
    }

    private fun showGeneratedImages(bitmaps: List<Bitmap>) {
        layoutResult.visibility = View.VISIBLE
        selectedGeneratedImageIndex = -1
        btnGenerate.text = "썸네일을 선택한 후 버튼을 눌러 주세요"

        val adapter = GeneratedThumbnailAdapter(bitmaps) { index ->
            selectedGeneratedImageIndex = index
            btnGenerate.text = "이 썸네일로 확정하기 →"
            btnGenerate.isEnabled = true
        }
        rvGeneratedImages.adapter = adapter
        Toast.makeText(this, "완료! 마음에 드는 썸네일을 선택해 주세요 😊", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("생성 실패")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()

        btnGenerate.text = "다시 생성하기"
        btnGenerate.isEnabled = true
    }

    // ───────────────────────────────────────────────
    // 이미지 미리보기 / 저장
    // ───────────────────────────────────────────────

    private fun updateImagePreviewContainer() {
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

        Toast.makeText(this, "레퍼런스 이미지 ${selectedImageUris.size}장 선택됨", Toast.LENGTH_SHORT).show()
    }

    private fun saveAndReturnImage(bitmap: Bitmap) {
        btnGenerate.isEnabled = false
        btnGenerate.text = "저장 중..."

        CoroutineScope(Dispatchers.Main).launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val file = File(cacheDir, "ai_thumbnail_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
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
                Toast.makeText(this@AiThumbnailActivity, "저장 실패. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                btnGenerate.isEnabled = true
                btnGenerate.text = "이 썸네일로 확정하기 →"
            }
        }
    }
}
