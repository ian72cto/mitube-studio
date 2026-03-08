package com.mitube.mlc
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LiveDefaultSettingsActivity : AppCompatActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerCaption: Spinner
    private lateinit var spinnerComments: Spinner
    
    // Checkboxes
    private lateinit var cbDvr: CheckBox
    private lateinit var cb360: CheckBox
    private lateinit var cbPaidPromo: CheckBox
    private lateinit var cbAlteredContent: CheckBox
    private lateinit var cbAutoChapters: CheckBox
    private lateinit var cbPlaces: CheckBox
    private lateinit var cbConcepts: CheckBox
    private lateinit var cbEmbed: CheckBox
    private lateinit var cbShowLikes: CheckBox
    private lateinit var cbChat: CheckBox
    private lateinit var cbChatReplay: CheckBox

    // Radio
    private lateinit var rgKids: RadioGroup
    private lateinit var rgLatency: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_default_settings)
        supportActionBar?.hide()

        // Initialize UI
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerCaption = findViewById(R.id.spinnerCaption)
        spinnerComments = findViewById(R.id.spinnerComments)
        
        cbDvr = findViewById(R.id.cbDvr)
        cb360 = findViewById(R.id.cb360)
        cbPaidPromo = findViewById(R.id.cbPaidPromo)
        cbAlteredContent = findViewById(R.id.cbAlteredContent)
        cbAutoChapters = findViewById(R.id.cbAutoChapters)
        cbPlaces = findViewById(R.id.cbPlaces)
        cbConcepts = findViewById(R.id.cbConcepts)
        cbEmbed = findViewById(R.id.cbEmbed)
        cbShowLikes = findViewById(R.id.cbShowLikes)
        cbChat = findViewById(R.id.cbChat)
        cbChatReplay = findViewById(R.id.cbChatReplay)
        rgKids = findViewById(R.id.rgKids)
        rgLatency = findViewById(R.id.rgLatency)

        setupSpinners()
        loadSettings()

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            saveSettings()
            Toast.makeText(this, "기본 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnPlaylist).setOnClickListener { btn ->
            val playlists = arrayOf("모든 영상", "라이브 스트리밍 저장소", "게임 방송 모음", "Vlog")
            android.app.AlertDialog.Builder(this)
                .setTitle("재생목록 선택")
                .setItems(playlists) { _, which ->
                    (btn as Button).text = playlists[which]
                    val prefs = getSharedPreferences("MLC_LIVE_DEFAULTS", Context.MODE_PRIVATE)
                    prefs.edit().putString("selected_playlist", playlists[which]).apply()
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun setupSpinners() {
        val categories = arrayOf(
            "1 - 영화/애니메이션",
            "2 - 자동차/교통",
            "10 - 음악",
            "15 - 반려동물/동물",
            "17 - 스포츠",
            "19 - 여행/이벤트",
            "20 - 게임",
            "21 - 브이로그",
            "22 - 인물/블로그",
            "23 - 코미디",
            "24 - 엔터테인먼트",
            "25 - 뉴스/정치",
            "26 - 노하우/스타일",
            "27 - 교육",
            "28 - 과학/기술",
            "29 - 비영리/사회운동"
        )
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val captions = arrayOf("없음 (None)", "미국 TV 방영 (Never Aired)", "FCC 면제 (FCC Exemption)")
        spinnerCaption.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, captions)

        val comments = arrayOf("모두 허용", "보류 후 검토", "사용 안함")
        spinnerComments.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, comments)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("MLC_LIVE_DEFAULTS", Context.MODE_PRIVATE)
        
        spinnerCategory.setSelection(prefs.getInt("category_idx", 0))
        spinnerCaption.setSelection(prefs.getInt("caption_idx", 0))
        spinnerComments.setSelection(prefs.getInt("comment_idx", 0))

        val savedPlaylist = prefs.getString("selected_playlist", "재생목록 선택 안함")
        findViewById<Button>(R.id.btnPlaylist).text = savedPlaylist

        cbDvr.isChecked = prefs.getBoolean("enableDvr", false) // User requested default unchecked
        cb360.isChecked = false // Hardcoded disabled
        cbPaidPromo.isChecked = prefs.getBoolean("paidPromo", false)
        cbAlteredContent.isChecked = prefs.getBoolean("alteredContent", false)
        cbAutoChapters.isChecked = prefs.getBoolean("autoChapters", true)
        cbPlaces.isChecked = prefs.getBoolean("places", true)
        cbConcepts.isChecked = prefs.getBoolean("concepts", true)
        cbEmbed.isChecked = prefs.getBoolean("embed", true)
        cbShowLikes.isChecked = prefs.getBoolean("showLikes", true)
        cbChat.isChecked = prefs.getBoolean("enableChat", true)
        cbChatReplay.isChecked = prefs.getBoolean("enableChatReplay", true)

        val isKids = prefs.getBoolean("isKids", false)
        if (isKids) {
            rgKids.check(R.id.rbKidsYes)
        } else {
            rgKids.check(R.id.rbKidsNo)
        }

        val latencyVal = prefs.getString("latency_val", "normal")
        when (latencyVal) {
            "low" -> rgLatency.check(R.id.rbLatencyLow)
            "ultraLow" -> rgLatency.check(R.id.rbLatencyUltra)
            else -> rgLatency.check(R.id.rbLatencyNormal)
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("MLC_LIVE_DEFAULTS", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("category_idx", spinnerCategory.selectedItemPosition)
            putInt("caption_idx", spinnerCaption.selectedItemPosition)
            putInt("comment_idx", spinnerComments.selectedItemPosition)
            
            putBoolean("enableDvr", cbDvr.isChecked)
            putBoolean("paidPromo", cbPaidPromo.isChecked)
            putBoolean("alteredContent", cbAlteredContent.isChecked)
            putBoolean("autoChapters", cbAutoChapters.isChecked)
            putBoolean("places", cbPlaces.isChecked)
            putBoolean("concepts", cbConcepts.isChecked)
            putBoolean("embed", cbEmbed.isChecked)
            putBoolean("showLikes", cbShowLikes.isChecked)
            putBoolean("enableChat", cbChat.isChecked)
            putBoolean("enableChatReplay", cbChatReplay.isChecked)
            putBoolean("isKids", rgKids.checkedRadioButtonId == R.id.rbKidsYes)
            
            // Extract actual string values for easy retrieval
            val latencyVal = when (rgLatency.checkedRadioButtonId) {
                R.id.rbLatencyLow -> "low"
                R.id.rbLatencyUltra -> "ultraLow"
                else -> "normal"
            }
            putString("latency_val", latencyVal)
            
            apply()
        }
    }
}
