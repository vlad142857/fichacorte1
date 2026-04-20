package com.example.fichacorte

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnNewFicha = findViewById<Button>(R.id.btnNewFicha)
        val btnProductionStatus = findViewById<Button>(R.id.btnProductionStatus)
        val btnInventory = findViewById<Button>(R.id.btnInventory)

        btnNewFicha.setOnClickListener {
            Toast.makeText(this, "Funcionalidade: Nova Ficha de Corte", Toast.LENGTH_SHORT).show()
        }

        btnProductionStatus.setOnClickListener {
            Toast.makeText(this, "Funcionalidade: Acompanhar Produção", Toast.LENGTH_SHORT).show()
        }

        btnInventory.setOnClickListener {
            Toast.makeText(this, "Funcionalidade: Estoque de Tecidos", Toast.LENGTH_SHORT).show()
        }
    }
}