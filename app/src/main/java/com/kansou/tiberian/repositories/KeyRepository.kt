package com.kansou.tiberian.repositories
import android.content.Context
import com.kansou.tiberian.model.KeyModel
import com.kansou.tiberian.utils.database
import org.jetbrains.anko.db.*

class KeyRepository(val context: Context) {
    fun findAll() : ArrayList<KeyModel> = context.database.use {
        val keys = ArrayList<KeyModel>()

        select("keys").orderBy("id") .parseList(object: MapRowParser<List<KeyModel>> {
            override fun parseRow(columns: Map<String, Any?>): List<KeyModel> {
                val id = columns.getValue("id")
                val account = columns.getValue("account")
                val issuer = columns.getValue("issuer")
                val secret = columns.getValue("secret")
                val algorithm = columns.getValue("algorithm")
                val period = columns.getValue("period")
                val type = columns.getValue("type")
                val uri = columns.getValue("uri")

                val key = KeyModel(id.toString().toLong(), account.toString(), issuer.toString(), secret.toString(), algorithm.toString(), period.toString(), type.toString(), uri.toString())

                keys.add(key)

                return keys
            }
        })

        keys
    }

    fun findById(id: Long) : KeyModel? = context.database.use {
        var key : KeyModel? = null
        select("keys")
            .whereArgs("(id = {id})", "id" to id)
            .parseOpt(object: MapRowParser<KeyModel> {
            override fun parseRow(columns: Map<String, Any?>): KeyModel {
                val id = columns.getValue("id")
                val account = columns.getValue("account")
                val issuer = columns.getValue("issuer")
                val secret = columns.getValue("secret")
                val algorithm = columns.getValue("algorithm")
                val period = columns.getValue("period")
                val type = columns.getValue("type")
                val uri = columns.getValue("uri")

                key = KeyModel(id.toString().toLong(), account.toString(), issuer.toString(), secret.toString(), algorithm.toString(), period.toString(), type.toString(), uri.toString())

                return key as KeyModel
            }
        })
        key
    }

    fun create(key: KeyModel) = context.database.use {
        insert("keys",
                    "account" to key.account,
                    "issuer" to key.issuer,
                    "secret" to key.secret,
                    "algorithm" to key.algorithm,
                    "period" to key.period,
                    "type" to key.type,
                    "uri" to key.uri)
    }

    fun update(key: KeyModel) = context.database.use {
        update("keys", "account" to key.account,
                "issuer" to key.issuer,
                "secret" to key.secret).whereArgs("id = {keyID}", "keyID" to key.id).exec()
    }

    fun deleteById(id: Long) = context.database.use {
        delete("keys","id = {keyID}", "keyID" to id)
    }
}