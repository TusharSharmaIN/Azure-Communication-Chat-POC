package com.android.azure_communication_chat_poc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_chat_details.*

class AddChatDetails : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_details)

        btn_start.setOnClickListener {
            if (et_name.text.toString().isNotEmpty() && et_thread_id.text.toString().isNotEmpty() &&
                et_token.text.toString().isNotEmpty() && et_resource_url.text.toString().isNotEmpty()) {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_NAME", et_name.text.toString())
                intent.putExtra("THREAD_ID", et_thread_id.text.toString())
                intent.putExtra("TOKEN", et_token.text.toString())
                intent.putExtra("RESOURCE_URL", et_resource_url.text.toString())
                startActivity(intent)
                finish()
            } else{
                Toast.makeText(this, "Please add all details", Toast.LENGTH_LONG).show()
            }
        }

    }
}