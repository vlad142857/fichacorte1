package com.example.fichacorte

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.html.*
import java.util.concurrent.CopyOnWriteArrayList

// Modelo de dados atualizado
data class FichaCorte(
    val id: Int,
    val referencia: String,
    val cliente: String,
    val tecido: String,
    val cor: String,
    val p: Int,
    val m: Int,
    val g: Int,
    val gg: Int,
    val folhas: Int,
    val metragem: String,
    val observacoes: String,
    var status: String = "No Corte" // Agora é var para podermos mudar
)

val database = CopyOnWriteArrayList<FichaCorte>()
var nextId = 1

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        // Página Inicial
        get("/") {
            call.respondHtml {
                head {
                    title { +"Painel Têxtil" }
                    style {
                        +"""
                            body { font-family: 'Segoe UI', sans-serif; margin: 0; background-color: #f0f2f5; }
                            .navbar { background: #007bff; padding: 1rem; color: white; display: flex; justify-content: space-between; }
                            .navbar a { color: white; text-decoration: none; font-weight: bold; margin-left: 15px; }
                            .container { max-width: 1100px; margin: 20px auto; background: white; padding: 25px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                            table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                            th, td { border-bottom: 1px solid #eee; padding: 12px; text-align: left; }
                            th { background: #f8f9fa; }
                            .status { padding: 4px 10px; border-radius: 15px; font-size: 0.8em; font-weight: bold; }
                            .status-cutting { background: #fff3cd; color: #856404; }
                            .status-sewing { background: #cce5ff; color: #004085; }
                            .status-finished { background: #d4edda; color: #155724; }
                            .btn-action { padding: 5px 8px; margin-right: 5px; text-decoration: none; border-radius: 4px; font-size: 0.8em; border: 1px solid #ccc; color: #333; background: #fefefe; cursor: pointer; }
                            .btn-print { background: #6c757d; color: white; border: none; }
                            .btn-add { background: #007bff; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; display: inline-block; margin-top: 15px; }
                        """.trimIndent()
                    }
                }
                body {
                    div("navbar") {
                        span { +"CONTROLE DE PRODUÇÃO TÊXTIL" }
                        div {
                            a(href = "/") { +"Painel" }
                            a(href = "/nova-ficha") { +"Nova Ficha" }
                        }
                    }
                    div("container") {
                        h2 { +"Acompanhamento de Ordens" }
                        table {
                            thead {
                                tr {
                                    th { +"REF" }
                                    th { +"Produto/Cor" }
                                    th { +"Total" }
                                    th { +"Status Atual" }
                                    th { +"Ações" }
                                }
                            }
                            tbody {
                                database.forEach { ficha ->
                                    tr {
                                        td { +ficha.referencia }
                                        td { +"${ficha.tecido} - ${ficha.cor}" }
                                        td { +(ficha.p + ficha.m + ficha.g + ficha.gg).toString() }
                                        td {
                                            val sClass = when(ficha.status) {
                                                "No Corte" -> "status-cutting"
                                                "Na Costura" -> "status-sewing"
                                                else -> "status-finished"
                                            }
                                            span("status $sClass") { +ficha.status }
                                        }
                                        td {
                                            // Botões para mudar status
                                            form(action = "/update-status/${ficha.id}", method = FormMethod.post, classes = "inline-form") {
                                                style = "display: inline;"
                                                if (ficha.status == "No Corte") {
                                                    button(type = ButtonType.submit, classes = "btn-action") { 
                                                        name = "newStatus"; value = "Na Costura"; +"→ Costura" 
                                                    }
                                                } else if (ficha.status == "Na Costura") {
                                                    button(type = ButtonType.submit, classes = "btn-action") { 
                                                        name = "newStatus"; value = "Finalizado"; +"→ Finalizar" 
                                                    }
                                                }
                                            }
                                            a(href = "/imprimir/${ficha.id}", target = "_blank", classes = "btn-action btn-print") { +"🖨️ PDF" }
                                        }
                                    }
                                }
                            }
                        }
                        a(href = "/nova-ficha", classes = "btn-add") { +"+ Nova Ficha de Corte" }
                    }
                }
            }
        }

        // Rota de Impressão (PDF/Imprimir)
        get("/imprimir/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val ficha = database.find { it.id == id }
            if (ficha == null) {
                call.respondText("Ficha não encontrada", status = HttpStatusCode.NotFound)
                return@get
            }

            call.respondHtml {
                head {
                    title { +"Ficha de Corte - ${ficha.referencia}" }
                    style {
                        +"""
                            body { font-family: 'Segoe UI', sans-serif; padding: 40px; }
                            .print-header { border: 2px solid #000; padding: 15px; text-align: center; margin-bottom: 20px; }
                            .data-row { display: flex; border-bottom: 1px solid #ccc; padding: 8px 0; }
                            .label { font-weight: bold; width: 180px; }
                            .grade-box { border-collapse: collapse; width: 100%; margin-top: 20px; }
                            .grade-box th, .grade-box td { border: 1px solid #000; padding: 10px; text-align: center; }
                            .obs { margin-top: 30px; border: 1px solid #ccc; padding: 10px; min-height: 100px; }
                            @media print { .no-print { display: none; } }
                        """.trimIndent()
                    }
                }
                body {
                    div("no-print") {
                        button {
                            attributes["onclick"] = "window.print()"
                            +"Clique aqui para Imprimir / Salvar PDF"
                        }
                        a(href = "/") { +" Voltar ao sistema" }
                        hr()
                    }
                    
                    div("print-header") {
                        h1 { +"FICHA DE CORTE - PRODUÇÃO" }
                        p { +"Referência: ${ficha.referencia} | Data: ${java.time.LocalDate.now()}" }
                    }

                    div("data-row") { div("label") { +"Cliente:" }; div { +ficha.cliente } }
                    div("data-row") { div("label") { +"Tecido:" }; div { +ficha.tecido } }
                    div("data-row") { div("label") { +"Cor/Variante:" }; div { +ficha.cor } }
                    div("data-row") { div("label") { +"Nº Folhas:" }; div { +ficha.folhas.toString() } }
                    div("data-row") { div("label") { +"Metragem:" }; div { +ficha.metragem } }

                    h3 { +"GRADE DE PRODUÇÃO" }
                    table("grade-box") {
                        tr { th { +"P" }; th { +"M" }; th { +"G" }; th { +"GG" }; th { +"TOTAL" } }
                        tr {
                            td { +ficha.p.toString() }
                            td { +ficha.m.toString() }
                            td { +ficha.g.toString() }
                            td { +ficha.gg.toString() }
                            td { b { +(ficha.p + ficha.m + ficha.g + ficha.gg).toString() } }
                        }
                    }

                    h3 { +"OBSERVAÇÕES" }
                    div("obs") { +ficha.observacoes }

                    div {
                        style = "margin-top: 50px; display: flex; justify-content: space-around;"
                        div { style = "border-top: 1px solid #000; width: 200px; text-align: center; padding-top: 5px;" { +"Ass. Cortador" } }
                        div { style = "border-top: 1px solid #000; width: 200px; text-align: center; padding-top: 5px;" { +"Ass. Responsável" } }
                    }
                }
            }
        }

        // Rota para mudar status
        post("/update-status/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val newStatus = call.receiveParameters()["newStatus"]
            if (id != null && newStatus != null) {
                database.find { it.id == id }?.status = newStatus
            }
            call.respondRedirect("/")
        }

        // Rota para Salvar e Outros (mantidos como antes)
        get("/nova-ficha") {
            call.respondHtml {
                head {
                    title { +"Nova Ficha de Corte" }
                    style {
                        +"""
                            body { font-family: 'Segoe UI', sans-serif; background-color: #f0f2f5; margin: 0; }
                            .container { max-width: 800px; margin: 30px auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                            h1 { color: #007bff; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
                            .section-title { font-weight: bold; color: #555; margin-top: 20px; margin-bottom: 10px; display: block; background: #e9ecef; padding: 5px 10px; }
                            .form-group { margin-bottom: 15px; display: flex; flex-wrap: wrap; gap: 10px; }
                            .field { flex: 1; min-width: 200px; }
                            label { display: block; margin-bottom: 5px; font-weight: 500; }
                            input, select { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
                            .grade-table { width: 100%; margin-top: 10px; }
                            .grade-table th { background: #f8f9fa; padding: 8px; }
                            .grade-table input { text-align: center; }
                            .btn-save { background: #28a745; color: white; padding: 15px 30px; border: none; border-radius: 5px; cursor: pointer; font-size: 1.1em; width: 100%; margin-top: 20px; font-weight: bold; }
                        """.trimIndent()
                    }
                }
                body {
                    div("container") {
                        a(href = "/") { +"← Voltar ao Painel" }
                        h1 { +"Ficha de Corte de Produção" }
                        form(action = "/save", method = FormMethod.post) {
                            span("section-title") { +"CABEÇALHO" }
                            div("form-group") {
                                div("field") { label { +"REF" }; input(type = InputType.text, name = "referencia") { required = true } }
                                div("field") { label { +"Cliente" }; input(type = InputType.text, name = "cliente") }
                            }
                            span("section-title") { +"TECIDOS" }
                            div("form-group") {
                                div("field") { label { +"Tecido" }; input(type = InputType.text, name = "tecido") }
                                div("field") { label { +"Cor" }; input(type = InputType.text, name = "cor") }
                            }
                            span("section-title") { +"GRADE" }
                            div("grade-table") {
                                table {
                                    tr { th { +"P" }; th { +"M" }; th { +"G" }; th { +"GG" } }
                                    tr {
                                        td { input(type = InputType.number, name = "p") { value = "0" } }
                                        td { input(type = InputType.number, name = "m") { value = "0" } }
                                        td { input(type = InputType.number, name = "g") { value = "0" } }
                                        td { input(type = InputType.number, name = "gg") { value = "0" } }
                                    }
                                }
                            }
                            span("section-title") { +"DETALHES" }
                            div("form-group") {
                                div("field") { label { +"Nº Folhas" }; input(type = InputType.number, name = "folhas") { value = "0" } }
                                div("field") { label { +"Metragem" }; input(type = InputType.text, name = "metragem") }
                            }
                            label { +"Observações" }
                            textArea { name = "observacoes"; attributes["rows"] = "3"; style = "width:100%" }
                            button(type = ButtonType.submit, classes = "btn-save") { +"SALVAR FICHA" }
                        }
                    }
                }
            }
        }

        post("/save") {
            val params = call.receiveParameters()
            database.add(FichaCorte(
                id = nextId++,
                referencia = params["referencia"] ?: "",
                cliente = params["cliente"] ?: "",
                tecido = params["tecido"] ?: "",
                cor = params["cor"] ?: "",
                p = params["p"]?.toIntOrNull() ?: 0,
                m = params["m"]?.toIntOrNull() ?: 0,
                g = params["g"]?.toIntOrNull() ?: 0,
                gg = params["gg"]?.toIntOrNull() ?: 0,
                folhas = params["folhas"]?.toIntOrNull() ?: 0,
                metragem = params["metragem"] ?: "",
                observacoes = params["observacoes"] ?: ""
            ))
            call.respondRedirect("/")
        }
    }
}
