package com.mitube.mlc

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThumbnailBottomSheetDialog(
    private val onGallerySelected: () -> Unit,
    private val onAiSelected: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_thumbnail, container, false)
        
        view.findViewById<LinearLayout>(R.id.llGalleryOption).setOnClickListener {
            onGallerySelected()
            dismiss()
        }
        
        view.findViewById<LinearLayout>(R.id.llAiOption).setOnClickListener {
            onAiSelected()
            dismiss()
        }

        return view
    }
}
