package com.example.digittulis

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.divyanshu.draw.widget.DrawView
import com.example.digittulis.databinding.ActivityMainBinding
import com.example.digittulis.util.clasificer

class MainActivity : AppCompatActivity() {
    private var digitclasficer = clasificer(this)
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.drawArea.setStrokeWidth(70.0f)
        binding.drawArea.setColor(Color.BLACK)


        binding.btnBack.setOnClickListener {
            binding.drawArea.clearCanvas()
        }



        binding.drawArea.setOnTouchListener { view, motionEvent ->
            binding.drawArea.onTouchEvent(motionEvent)

            if(motionEvent.action == MotionEvent.ACTION_UP){
              klasifikasi()
            }
            true
        }

        digitclasficer
            .init()
            .addOnFailureListener { Log.d("fail","gagal") }
    }

    private fun klasifikasi(){
        val bitmap = binding.drawArea.getBitmap()


        if ((bitmap != null) && (digitclasficer.isinitialize)) {
            digitclasficer
                .classifyAsync(bitmap)
                .addOnSuccessListener { resultText ->
                    binding.tvPred.text = resultText
                    Log.d("result",resultText.toString())
                }
        }

    }
}