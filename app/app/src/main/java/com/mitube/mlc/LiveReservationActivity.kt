package com.mitube.mlc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.mitube.mlc.api.YouTubeLiveApiManager
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult

class LiveReservationActivity : AppCompatActivity() {

    private var selectedThumbnailUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleThumbnailSelected(uri)
        }
    }

    private val aiThumbnailLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("EXTRA_THUMBNAIL_URI")
            if (uriString != null) {
                handleThumbnailSelected(Uri.parse(uriString))
            }
        }
    }

    private fun handleThumbnailSelected(uri: Uri) {
        selectedThumbnailUri = uri
        findViewById<ImageView>(R.id.ivThumbnailPreview).setImageURI(uri)
        findViewById<TextView>(R.id.tvThumbnailHint).visibility = View.GONE
    }

    private lateinit var apiManager: YouTubeLiveApiManager

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etTags: EditText
    private lateinit var rgPrivacy: RadioGroup
    private lateinit var rgParticipant: RadioGroup
    private lateinit var cbReactions: CheckBox
    private lateinit var cbSlowMode: CheckBox
    private lateinit var etSlowModeSeconds: EditText
    private lateinit var layoutSlowModeSeconds: LinearLayout
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_reservation)
        supportActionBar?.hide()

        val token = intent.getStringExtra("channel_token") ?: ""
        if (token.isEmpty()) {
            Toast.makeText(this, "채널 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        apiManager = YouTubeLiveApiManager(token)

        initViews()

        findViewById<ImageButton>(R.id.btnMenuSettings).setOnClickListener {
            startActivity(Intent(this, LiveDefaultSettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnScheduleLive).setOnClickListener {
            showDateTimePicker()
        }
        findViewById<Button>(R.id.btnStartLive).setOnClickListener {
            handleCreateBroadcast(null)
        }
    }

    private fun showDateTimePicker() {
        val c = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(this, { _, year, month, day ->
            android.app.TimePickerDialog(this, { _, hour, minute ->
                val date = java.util.Calendar.getInstance()
                date.set(year, month, day, hour, minute, 0)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val isoDate = format.format(date.time)
                handleCreateBroadcast(isoDate)
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), false).show()
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etTags = findViewById(R.id.etTags)
        rgPrivacy = findViewById(R.id.rgPrivacy)
        rgParticipant = findViewById(R.id.rgParticipant)
        cbReactions = findViewById(R.id.cbReactions)
        cbSlowMode = findViewById(R.id.cbSlowMode)
        etSlowModeSeconds = findViewById(R.id.etSlowModeSeconds)
        layoutSlowModeSeconds = findViewById(R.id.layoutSlowModeSeconds)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<View>(R.id.layoutThumbnail).setOnClickListener {
            val bottomSheet = ThumbnailBottomSheetDialog(
                onGallerySelected = {
                    pickImageLauncher.launch("image/*")
                },
                onAiSelected = {
                    val intent = Intent(this, AiThumbnailActivity::class.java)
                    aiThumbnailLauncher.launch(intent)
                }
            )
            bottomSheet.show(supportFragmentManager, "ThumbnailBottomSheet")
        }

        cbSlowMode.setOnCheckedChangeListener { _, isChecked ->
            layoutSlowModeSeconds.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun handleCreateBroadcast(scheduledTime: String?) {
        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val privacy = when (rgPrivacy.checkedRadioButtonId) {
            R.id.rbPrivacyPublic -> "public"
            R.id.rbPrivacyUnlisted -> "unlisted"
            R.id.rbPrivacyPrivate -> "private"
            else -> "unlisted"
        }
        val tags = etTags.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // Chat parameters
        val participantMode = when (rgParticipant.checkedRadioButtonId) {
            R.id.rbAllUsers -> "allUsers"
            R.id.rbSubscribers -> "subscribers"
            R.id.rbApproved -> "approvedUsers"
            else -> "allUsers"
        }
        val slowModeSeconds = if (cbSlowMode.isChecked) etSlowModeSeconds.text.toString().toIntOrNull() ?: 0 else 0
        val allowReactions = cbReactions.isChecked

        // Default parameters from SharedPreferences & Current UI Options
        val prefs = getSharedPreferences("MLC_LIVE_DEFAULTS", Context.MODE_PRIVATE)
        val isKids = prefs.getBoolean("isKids", false)
        val dvr = prefs.getBoolean("enableDvr", false)
        val latency = prefs.getString("latencyPreference", "normal") ?: "normal"
        val chatEnable = prefs.getBoolean("enableChat", true)
        val embed = prefs.getBoolean("embed", true)

        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "방송을 생성 중입니다..."

        lifecycleScope.launch {
            val broadcastId = apiManager.createLiveBroadcast(
                title = title,
                description = desc,
                privacyStatus = privacy,
                isMadeForKids = isKids,
                scheduledStartTime = scheduledTime,
                enableDvr = dvr,
                latencyPreference = latency,
                enableChat = chatEnable,
                enableEmbed = embed,
                tags = tags
            )

            if (broadcastId == null) {
                tvStatus.text = "방송 생성 실패!"
                return@launch
            }

            if (scheduledTime != null) {
                tvStatus.text = "방송 예약 완료! 리스트로 이동합니다."
                val targetIntent = Intent(this@LiveReservationActivity, ReservationListActivity::class.java).apply {
                    putExtra("channel_token", intent.getStringExtra("channel_token"))
                }
                startActivity(targetIntent)
                finish()
            } else {
                tvStatus.text = "스트림 키 발급 중..."
                val streamInfo = apiManager.createLiveStream(title)
                if (streamInfo == null) {
                    tvStatus.text = "스트림 발급 실패!"
                    return@launch
                }
                tvStatus.text = "방송과 스트림 바인딩 중..."

                val bound = apiManager.bindBroadcastToStream(broadcastId, streamInfo.id)
                if (bound) {
                    tvStatus.text = "방송 설정 완료! 카메라로 이동합니다."
                    val intent = Intent(this@LiveReservationActivity, LiveCameraActivity::class.java).apply {
                        putExtra("EXTRA_RTMP_URL", streamInfo.rtmpUrl)
                        putExtra("EXTRA_STREAM_KEY", streamInfo.streamKey)
                        putExtra("EXTRA_BROADCAST_ID", broadcastId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    tvStatus.text = "바인딩 실패!"
                }
            }
        }
    }
}
