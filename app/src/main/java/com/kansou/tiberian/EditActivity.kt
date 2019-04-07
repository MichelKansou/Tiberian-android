package com.kansou.tiberian

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import com.kansou.tiberian.model.KeyModel
import com.kansou.tiberian.repositories.KeyRepository
import kotlinx.android.synthetic.main.activity_edit.*


class EditActivity : Activity() {

    private var tmpKey: KeyModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        tmpKey = intent.getParcelableExtra("key") as? KeyModel
        if (tmpKey != null) {
            account.setText(tmpKey!!.account)
            issuer.setText(tmpKey!!.issuer)
            secret.setText(tmpKey!!.secret)
        }

        cancelBtn.setOnClickListener {
            finish()
        }

        saveBtn.setOnClickListener{
            if (tmpKey != null) {
                tmpKey!!.account = account.text.toString()
                tmpKey!!.issuer = issuer.text.toString()
                tmpKey!!.secret = secret.text.toString()
                KeyRepository(this).update(tmpKey!!)
                finish()
            }
        }
    }


    override fun onResume() {
        super.onResume()
    }
}

