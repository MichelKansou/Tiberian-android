package com.kansou.tiberian

import android.animation.*
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kansou.tiberian.R.color.*
import com.kansou.tiberian.model.KeyModel
import com.kansou.tiberian.model.QRcode
import com.kansou.tiberian.repositories.KeyRepository
import com.kansou.tiberian.utils.ScannerUtilities
import com.kansou.tiberian.utils.TwoFactorAuthUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.key_row.view.*
import kotlinx.android.synthetic.main.row_swipe.view.*
import android.view.animation.DecelerateInterpolator
import android.animation.ObjectAnimator
import android.R.attr.animation






class MainActivity : Activity() {

    private var fabBtn: FloatingActionButton? = null
    private var qrcodeResult: String? = null

    private val RC_BARCODE_CAPTURE = 9001
    private val TAG = "MAIN_ACTIVITY"
    private lateinit var tempdataBase: KeyModel
    private lateinit var mCounterHandler: Handler
    private var mCounter = 0

    private var clipboardManager: ClipboardManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabBtn = findViewById(R.id.fab) as FloatingActionButton

        fabBtn!!.setOnClickListener {
            val intent = Intent(this@MainActivity, ScannerActivity::class.java)
            Log.d(TAG, "Start Scanner Activity")
            startActivityForResult(intent, RC_BARCODE_CAPTURE)
        }

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        mCounterHandler = Handler()
        generateTable()
    }

    override fun onResume() {
        super.onResume()
        if (!qrcodeResult.isNullOrEmpty()) {
            val qrCodeUri = Uri.parse(qrcodeResult)
            Log.d(TAG, "URI " + qrcodeResult)
            if (qrCodeUri != null) {
                tempdataBase = ScannerUtilities().parseSecret(qrCodeUri)!!
                if (tempdataBase != null) {
                    // Save Key in DB
                    val key = KeyRepository(this).create(tempdataBase)
                    Log.d(TAG, "Key ID : " + key.toInt())
                    tempdataBase.id = key
                    // Add Key to Table
                    addRow(tempdataBase)
                    qrcodeResult = null
                }
            }
        }
        if (keysTable.childCount > 0) {
            updateRows()
            stopRepeatingTask()
            startRepeatingTask()
        }
    }

    private fun generateTable() {
        val keys: ArrayList<KeyModel> = KeyRepository(this).findAll()
        keys.forEach{
            addRow(it)
        }
    }



    private fun addRow(key: KeyModel) {
        val row = View.inflate(this, R.layout.key_row, null)
        row.generatedPass.text = TwoFactorAuthUtil().generateCurrentNumber(key.secret)
        row.issuer.text = key.issuer
        row.account.text = key.account
        row.id = key.id.toInt()
        row.swipeLayout.removeBtn.setOnClickListener {
            Log.d(TAG, "Remove row")
            removeRow(row.id)
            keysTable.removeView(row)
        }
        row.swipeLayout.editBtn.setOnClickListener {
            Log.d(TAG, "Edit row " + key.account)
            val intent = Intent(this@MainActivity, EditActivity::class.java)
            intent.putExtra("key", key)
            startActivity(intent)
        }
        row.linearLayoutKeyRow.setOnClickListener {
            animateBackgroundRow(it, android.R.color.transparent, colorAccent)
            animateBackgroundRow(it, colorAccent, android.R.color.transparent)
            animateTextRow(it)
            clipboardManager?.primaryClip = ClipData.newPlainText("auth-code", it.generatedPass.text)
        }
        keysTable.addView(row)
    }

    private fun animateBackgroundRow(view : View, background_from: Int, background_to: Int) {

        val anim = ValueAnimator()
        anim.setIntValues(ContextCompat.getColor(this, background_from), ContextCompat.getColor(this, background_to))
        anim.setEvaluator(ArgbEvaluator())
        anim.addUpdateListener {
            view.linearLayoutKeyRow.setBackgroundColor(it.animatedValue as Int)
        }
        anim.duration = 700
        anim.start()
    }

    private fun animateTextRow(view: View) {
        val tempIssuer = view.issuer.text
        val tempAccount = view.account.text

        val anim = AlphaAnimation(1.0f, 1.0f)
        anim.duration = 800

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) { }
            override fun onAnimationEnd(animation: Animation?) {
                view.issuer.text = tempIssuer
                view.account.text = tempAccount
            }
            override fun onAnimationStart(animation: Animation?) {
                view.issuer.text = ""
                view.account.text = "Copied Code"
            }
        })

        view.startAnimation(anim)

    }

    fun updateRows() {
        Log.d(TAG, "Update rows")
        mCounter = 0
        progressBar.progress = 0
        animateProgressBar()
        for (i in 1..keysTable.childCount) {
            val currentRowKey = keysTable.getChildAt(i.minus(1))
            if (currentRowKey != null) {
                val key = KeyRepository(this).findById(currentRowKey.id.toLong())
                if (key != null) {
                    currentRowKey.generatedPass.text = TwoFactorAuthUtil().generateCurrentNumber(key.secret)
                    currentRowKey.account.text = key.account
                    currentRowKey.issuer.text = key.issuer
                }
            }
        }
    }

    fun removeRow(rowId: Int) {
        Log.d(TAG, "Remove rows " + rowId)
        KeyRepository(this).deleteById(rowId.toLong())
    }

    private var mCounterChecker: Runnable = object : Runnable {
        override fun run() {
            try {
                if (mCounter >= 30) updateRows()
                if (mCounter <= 30) mCounter = mCounter + 1  //this function can change value of mCounter.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mCounterHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun animateProgressBar() {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 30000)
        animation.duration = 30000
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun startRepeatingTask() {
        mCounterChecker.run()
    }

    private fun stopRepeatingTask() {
        mCounterHandler.removeCallbacks(mCounterChecker)
    }


    companion object {
        val BarcodeObject = "Barcode"
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * [.RESULT_CANCELED] if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     *
     *
     *
     * You will receive this call immediately before onResume() when your
     * activity is re-starting.
     *
     *
     *
     * @param requestCode The integer request code originally supplied to
     * startActivityForResult(), allowing you to identify who this
     * result came from.
     * @param resultCode  The integer result code returned by the child activity
     * through its setResult().
     * @param data        An Intent, which can return result data to the caller
     * (various data can be attached to Intent "extras").
     * @see .startActivityForResult
     *
     * @see .createPendingResult
     *
     * @see .setResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    val barcode = data.getParcelableExtra<QRcode>(BarcodeObject)
                    qrcodeResult = barcode.data.displayValue
                    Log.d(TAG, "Barcode read: " + barcode.data.displayValue)
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null")
                }
            } else {
                Log.d(TAG, "Error reading barcode");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        stopRepeatingTask()
    }

}
