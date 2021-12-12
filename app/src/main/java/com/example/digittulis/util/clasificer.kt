package com.example.digittulis.util

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Throws
import kotlin.math.log

class clasificer(private val context: Context) {
        //tflite interpreter
        private var interpreter : Interpreter? = null
        var isinitialize = false
        private set


        //executor pada background
        private val executorservice : ExecutorService = Executors.newCachedThreadPool()
        private var inputimgwidth : Int = 0
        private var inputimgheight : Int = 0
        private var modelinputsize : Int = 0


        fun init(): Task<Void>{
                val task = TaskCompletionSource<Void>()
                executorservice.execute {
                        try{
                                initinterpreter()
                                task.setResult(null)
                        }catch (e : IOException){
                                task.setException(e)
                        }
                }
                return task.task
        }

        @Throws(IOException::class)
        private fun initinterpreter(){
                //mengambil asset tf lite
                val assetm = context.assets
                val model = loadmodel(assetm,"digit.tflite")
                val interprete = Interpreter(model)

                //membaca bentuk gambar
                val inputshape = interprete.getInputTensor(0).shape()
                inputimgwidth = inputshape[1]
                inputimgheight = inputshape[2]
                modelinputsize = FLOAT_TYPE_SIZE * inputimgwidth * inputimgheight * PIXEL_SIZE
                //4 * inputwidthtensor * inputsizetensor * 1

                this.interpreter = interprete
                isinitialize = true
        }

        @Throws(IOException::class)
        private fun loadmodel(assetManager: AssetManager,filename: String):ByteBuffer{
                val filedescriptor = assetManager.openFd(filename) //membuka file
                val inputstream = FileInputStream(filedescriptor.fileDescriptor)
                val filechannel = inputstream.channel
                val startOffset = filedescriptor.startOffset
                val declaredLength = filedescriptor.declaredLength
                return filechannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }



        private fun clasifikasi(bitmap: Bitmap): String{

                //preprocessing
                val resizedimg = Bitmap.createScaledBitmap(
                        bitmap,
                        inputimgwidth,
                        inputimgheight,
                        true
                )

                val byteBuffer = konversibitmaptobytebuffer(resizedimg)

                val output = Array(1){
                        FloatArray(OUTPUT_CLASSES_COUNT)
                }

                interpreter?.run(byteBuffer,output)

                val result = output[0]
                val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1
                val resultString = "Angka tercetak : %d\n akurasi: %2f".format(maxIndex, result[maxIndex])
                Log.d("result",resultString)
                return resultString

        }

        fun classifyAsync(bitmap: Bitmap): Task<String> {
                val task = TaskCompletionSource<String>()
                executorservice.execute {
                        val result = clasifikasi(bitmap)
                        task.setResult(result)

                }
                return task.task
        }



        private fun konversibitmaptobytebuffer(bitmap: Bitmap): ByteBuffer{
                val byteBuffer = ByteBuffer.allocateDirect(modelinputsize)
                byteBuffer.order(ByteOrder.nativeOrder())



                val pixels = IntArray(inputimgwidth * inputimgheight)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                for (pixelValue in pixels) {
                        val r = (pixelValue shr 16 and 0xFF)
                        val g = (pixelValue shr 8 and 0xFF)
                        val b = (pixelValue and 0xFF)

                        // Convert RGB to grayscale and normalize pixel value to [0..1].
                        val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
                        byteBuffer.putFloat(normalizedPixelValue)
                }
                return byteBuffer
        }


        companion object{
                private const val FLOAT_TYPE_SIZE = 4
                private const val PIXEL_SIZE = 1
                private const val OUTPUT_CLASSES_COUNT = 10
        }
}