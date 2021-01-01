/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.sample.activity

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.*
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.FilterAdjuster
import jp.co.cyberagent.android.gpuimage.sample.R
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private var filterAdjuster: FilterAdjuster? = null
    private val gpuImageView: GPUImageView by lazy { findViewById<GPUImageView>(R.id.gpuimage) }
    private val seekBar: SeekBar by lazy { findViewById<SeekBar>(R.id.seekBar) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val f = gpuImageView.filter
                if (f is GPUImageFilterGroup) {
                    try {
                        val contrast = range(progress, 0.0f, 2.0f)
                        val lineSize = range(progress, 0.0f, 5.0f)
                        val filters = f.filters
                        filters.clear()
                        val sobelFilter = GPUImageDirectionalSobelEdgeDetectionFilter()
                        sobelFilter.setLineSize(lineSize)
                        gpuImageView.filter = GPUImageFilterGroup(mutableListOf(GPUImageContrastFilter(contrast), sobelFilter, GPUImageGrayscaleFilter()))
                    } catch (e: Exception) {
                        Log.e("test", "onProgressChanged: ", e)
                    }
                } else {
                    filterAdjuster?.adjust(progress)
                }
                gpuImageView.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<View>(R.id.button_choose_filter).setOnClickListener {
            GPUImageFilterTools.showDialog(this) { filter ->
                switchFilterTo(filter)
                gpuImageView.requestRender()
            }
        }
        findViewById<View>(R.id.button_save).setOnClickListener { saveImage() }
        gpuImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        startPhotoPicker()
    }

    private fun range(percentage: Int, start: Float, end: Float): Float {
        return (end - start) * percentage / 100.0f + start
    }

    private fun range(percentage: Int, start: Int, end: Int): Int {
        return (end - start) * percentage / 100 + start
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PICK_IMAGE -> if (resultCode == RESULT_OK) {
                gpuImageView.setImage(data!!.data)
            } else {
                finish()
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startPhotoPicker() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE)
    }

    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val path: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(path, "GPUImage/$fileName")
        gpuImageView.saveToPictures(file, true) { uri ->
            Toast.makeText(this, "Saved: $uri", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchFilterTo(filter: GPUImageFilter) {
        if (gpuImageView.filter == null || gpuImageView.filter.javaClass != filter.javaClass) {
            gpuImageView.filter = filter
            filterAdjuster = FilterAdjuster(filter)
            if (filterAdjuster!!.canAdjust()) {
                seekBar.visibility = View.VISIBLE
                filterAdjuster!!.adjust(seekBar.progress)
            } else {
                seekBar.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 1
    }
}
