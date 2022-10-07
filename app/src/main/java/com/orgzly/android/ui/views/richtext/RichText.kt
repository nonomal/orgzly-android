package com.orgzly.android.ui.views.richtext

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.ImageLoader
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.OrgFormatter

class RichText(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    fun interface OnUserTextChangeListener {
        fun onUserTextChange(str: String)
    }

    data class Listeners(
        var onUserTextChange: OnUserTextChangeListener? = null)

    private val listeners = Listeners()

    fun setOnUserTextChangeListener(listener: OnUserTextChangeListener) {
        listeners.onUserTextChange = listener
    }

    private val sourceBackgroundColor: Int by lazy {
        context.styledAttributes(intArrayOf(R.attr.colorSecondaryContainer)) { typedArray ->
            typedArray.getColor(0, 0)
        }
    }

    data class Attributes(
        val viewId: Int = 0,
        val editId: Int = 0,
        val parseCheckboxes: Boolean = true,
        val linkify: Boolean = true,
        val editable: Boolean = true,
        val inputType: Int = InputType.TYPE_NULL,
        val imeOptions: Int = 0,
        val hint: String? = null,
        val textSize: Int = 0,
        val paddingHorizontal: Int = 0,
        val paddingVertical: Int = 0
    )

    private lateinit var attributes: Attributes

    private val richTextEdit: RichTextEdit
    private val richTextView: RichTextView

    init {
        parseAttrs(attrs)

        inflate(getContext(), R.layout.rich_text, this)

        richTextEdit = findViewById(R.id.rich_text_edit)
        richTextView = findViewById(R.id.rich_text_view)

        richTextEdit.apply {
            if (attributes.editId != 0) {
                id = attributes.editId
            }

            setCommonAttributes(this)

            inputType = attributes.inputType
            imeOptions = attributes.imeOptions

            // Wrap lines when editing. Doesn't work when set in XML?
            setHorizontallyScrolling(false)
            maxLines = Integer.MAX_VALUE

            if (AppPreferences.highlightEditedRichText(context)) {
                setBackgroundColor(sourceBackgroundColor)
            } else {
                setBackgroundColor(0)
            }

            // If RichTextEdit loses the focus, switch to view mode
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    toViewMode(true)
                }
            }
        }

        richTextView.apply {
            if (attributes.viewId != 0) {
                id = attributes.viewId
            }

            setCommonAttributes(this)

            if (attributes.editable) {
                setTextIsSelectable(true)
            }

            setOnTapUpListener { _, _, charOffset ->
                if (attributes.editable) {
                    toEditMode(charOffset)
                }
            }

            setOnUserTextChangeListener { str ->
                setSourceText(str)
                listeners.onUserTextChange?.onUserTextChange(str)
            }
        }
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            context.styledAttributes(attrs, R.styleable.RichText) { typedArray ->
                readAttributes(typedArray)
            }
        }
    }

    private fun readAttributes(typedArray: TypedArray) {
        typedArray.run {
            attributes = Attributes(
                getResourceId(R.styleable.RichText_view_id, 0),
                getResourceId(R.styleable.RichText_edit_id, 0),
                getBoolean(R.styleable.RichText_parse_checkboxes, true),
                getBoolean(R.styleable.RichText_linkify, true),
                getBoolean(R.styleable.RichText_editable, true),
                getInteger(R.styleable.RichText_android_inputType, InputType.TYPE_NULL),
                getInteger(R.styleable.RichText_android_imeOptions, 0),
                getString(R.styleable.RichText_android_hint),
                getDimensionPixelSize(R.styleable.RichText_android_textSize, 0),
                getDimensionPixelSize(R.styleable.RichText_paddingHorizontal, 0),
                getDimensionPixelSize(R.styleable.RichText_paddingVertical, 0),
            )
        }
    }

    private fun setCommonAttributes(view: TextView) {
        view.hint = attributes.hint

        if (attributes.textSize > 0) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, attributes.textSize.toFloat())
        }

        if (attributes.paddingHorizontal > 0 || attributes.paddingVertical > 0) {
            view.setPadding(
                attributes.paddingHorizontal,
                attributes.paddingVertical,
                attributes.paddingHorizontal,
                attributes.paddingVertical)
        }
    }

    fun setSourceText(text: CharSequence?) {
        richTextEdit.setText(text)

        if (richTextView.visibility == View.VISIBLE) {
            parseAndSetViewText()
        }
    }

    fun getSourceText(): CharSequence? {
        return richTextEdit.text
    }

    fun setVisibleText(text: CharSequence) {
        richTextView.text = text
    }

    fun toEditMode(charOffset: Int) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "editable:${attributes.editable}", charOffset)

        richTextEdit.activate(charOffset)
        richTextView.deactivate()
    }

    private fun toViewMode(reparseSource: Boolean = false) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "reparseSource:$reparseSource")

        if (reparseSource) {
            parseAndSetViewText()
        }

        richTextView.activate()
        richTextEdit.deactivate()
    }

    private fun parseAndSetViewText() {
        val source = richTextEdit.text

        if (source != null) {
            val parsed = OrgFormatter.parse(source, context, attributes.linkify, attributes.parseCheckboxes)

            richTextView.setText(parsed, TextView.BufferType.SPANNABLE)

            ImageLoader.loadImages(richTextView)

        } else {
            richTextView.text = null
        }
    }

    fun setTypeface(typeface: Typeface) {
        richTextView.typeface = typeface
        richTextEdit.typeface = typeface
    }

    fun setMaxLines(lines: Int) {
        if (lines < Integer.MAX_VALUE) {
            richTextView.maxLines = lines
            richTextView.ellipsize = TextUtils.TruncateAt.END
        } else {
            richTextView.maxLines = Integer.MAX_VALUE
            richTextView.ellipsize = null
        }
    }

    fun setOnEditorActionListener(any: TextView.OnEditorActionListener) {
        richTextEdit.setOnEditorActionListener(any)
    }

    companion object {
        val TAG: String = RichText::class.java.name
    }
}