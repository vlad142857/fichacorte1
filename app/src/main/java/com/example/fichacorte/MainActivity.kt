package com.example.fichacorte

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// O mesmo modelo de dados do servidor
data class FichaCorte(
    val id: Int,
    val referencia: String,
    val cliente: String,
    val status: String
)

// Interface para falar com o Servidor
interface FichaApi {
    @GET("api/fichas")
    fun getFichas(): Call<List<FichaCorte>>
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Criando uma tela simples via código para teste rápido
        val textView = TextView(this).apply {
            textSize = 20f
            setPadding(50, 50, 50, 50)
            text = "Carregando produção..."
        }
        setContentView(textView)

        // Configura a conexão com o servidor (IP especial para emulador Android)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") 
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(FichaApi::class.java)

        // Busca os dados
        api.getFichas().enqueue(object : Callback<List<FichaCorte>> {
            override fun onResponse(call: Call<List<FichaCorte>>, response: Response<List<FichaCorte>>) {
                if (response.isSuccessful) {
                    val fichas = response.body()
                    val resumo = StringBuilder("📋 PRODUÇÃO ATUAL:\n\n")
                    fichas?.forEach {
                        resumo.append("REF: ${it.referencia}\nStatus: ${it.status}\n-----------\n")
                    }
                    textView.text = resumo.toString()
                }
            }

            override fun onFailure(call: Call<List<FichaCorte>>, t: Throwable) {
                textView.text = "Erro ao conectar no servidor!\nVerifique se o 'Rodar Site' está ligado.\n\nDetalhe: ${t.message}"
            }
        })
    }
}
