package com.kansou.tiberian

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.snackbar.Snackbar

import java.io.IOException

import com.kansou.tiberian.camera.CameraSource
import com.kansou.tiberian.camera.GraphicOverlay
import com.kansou.tiberian.camera.CameraSourcePreview
import com.kansou.tiberian.model.QRcode


class ScannerActivity : AppCompatActivity(), BarcodeGraphicTracker.BarcodeUpdateListener {
    // intent request code to handle updating play services if needed.
    private val RC_HANDLE_GMS = 9001

    // permission request codes need to be < 256
    private val RC_HANDLE_CAMERA_PERM = 2

    private lateinit var btn: ImageView

    // constants used to pass extra data in the intent
    val AutoFocus = "AutoFocus"
    val UseFlash = "UseFlash"
    val BarcodeObject = "Barcode"

    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay<BarcodeGraphic>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        mPreview = findViewById(R.id.preview) as CameraSourcePreview
        mGraphicOverlay = findViewById(R.id.graphicOverlay) as GraphicOverlay<BarcodeGraphic>

        btn = findViewById(R.id.closeButton) as ImageView
        btn!!.setOnClickListener {
            finish()
        }

        // read parameters from the intent used to launch the activity.
        val autoFocus = intent.getBooleanExtra(AutoFocus, false)
        val useFlash = intent.getBooleanExtra(UseFlash, false)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash)
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w("SCANNER_ACTIVITY", "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf<String>(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        val listener = findViewById(R.id.topLayout) as View

        listener!!.setOnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
            val snack = Snackbar.make(mGraphicOverlay!!, R.string.permission_camera_rationale,
                    Snackbar.LENGTH_INDEFINITE)
            snack.setAction(R.string.ok, View.OnClickListener {
                // executed when DISMISS is clicked
                Log.d("MAIN_ACTIVITY", "Start Scanner Activity")
            })
            snack.show()
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource(autoFocus: Boolean, useFlash: Boolean) {
        val context = applicationContext

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(mGraphicOverlay!!, this)
        barcodeDetector.setProcessor(
                MultiProcessor.Builder(barcodeFactory).build())

        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w("SCANNER_ACTIVITY", "Detector dependencies are not yet available.")

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowstorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w("SCANNER_ACTIVITY", getString(R.string.low_storage_error))
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        var builder: CameraSource.Builder = CameraSource.Builder(applicationContext, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedFps(30.0f)

        mCameraSource = builder
                .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else null)
                .build()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        if (mPreview != null) {
            mPreview!!.stop()
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mPreview != null) {
            mPreview!!.release()
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d("SCANNER_ACTIVITY", "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("SCANNER_ACTIVITY", "Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            val useFlash = intent.getBooleanExtra(UseFlash, false)
            createCameraSource(true, useFlash)
            return
        }

        Log.e("SCANNER_ACTIVITY", "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {

        if (mCameraSource != null && mPreview != null) {
            try {
                mPreview!!.start(mCameraSource, mGraphicOverlay)
            } catch (e: IOException) {
                Log.e("SCANNER_ACTIVITY", "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }

        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private fun onTap(rawX: Float, rawY: Float): Boolean {
        // Find tap point in preview frame coordinates.
        val location = IntArray(2)
        mGraphicOverlay!!.getLocationOnScreen(location)
        val x = (rawX - location[0]) / mGraphicOverlay!!.getWidthScaleFactor()
        val y = (rawY - location[1]) / mGraphicOverlay!!.getHeightScaleFactor()

        // Find the barcode whose center is closest to the tapped point.
        var best: Barcode? = null
        var bestDistance = java.lang.Float.MAX_VALUE
        for (graphic in mGraphicOverlay!!.getGraphics()) {
            val barcode = graphic.barcode
            if (barcode!!.boundingBox.contains(x.toInt(), y.toInt())) {
                // Exact hit, no need to keep looking.
                best = barcode
                break
            }
            val dx = x - barcode.boundingBox.centerX()
            val dy = y - barcode.boundingBox.centerY()
            val distance = dx * dx + dy * dy  // actually squared distance
            if (distance < bestDistance) {
                best = barcode
                bestDistance = distance
            }
        }
        if (best != null) {
            val data = Intent()
            data.putExtra(BarcodeObject, QRcode(best))
            setResult(CommonStatusCodes.SUCCESS, data)
            finish()
            return true
        }
        return false

    }

    private inner class CaptureGestureListener : GestureDetector.SimpleOnGestureListener() {
      /*  override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e.rawX, e.rawY) || super.onSingleTapConfirmed(e)
        }*/
    }

    override fun onBarcodeDetected(barcode: Barcode?) {
        //do something with barcode data returned
        if (barcode != null) {
            val data = Intent()
            data.putExtra(BarcodeObject, QRcode(barcode))
            setResult(CommonStatusCodes.SUCCESS, data)
            Log.w("SCANNER_ACTIVITY", barcode!!.displayValue.toString())
            finish()
        }

    }
}
