package com.example.fichacorte

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils

// Definição da Tabela no Banco de Dados
object FichasTable : Table("fichas") {
    val id = integer("id").autoIncrement()
    val referencia = varchar("referencia", 50)
    val cliente = varchar("cliente", 100)
    val tecido = varchar("tecido", 100)
    val cor = varchar("cor", 50)
    val p = integer("p")
    val m = integer("m")
    val g = integer("g")
    val gg = integer("gg")
    val folhas = integer("folhas")
    val metragem = varchar("metragem", 20)
    val observacoes = text("observacoes")
    val status = varchar("status", 20).default("No Corte")
    
    override val primaryKey = PrimaryKey(id)
}

@Serializable
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
    var status: String
)

fun main() {
    Database.connect("jdbc:h2:./fichacorte_db;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    transaction {
        SchemaUtils.create(FichasTable)
    }

    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/api/fichas") {
            val fichas = transaction {
                FichasTable.selectAll().map {
                    FichaCorte(
                        it[FichasTable.id], it[FichasTable.referencia], it[FichasTable.cliente],
                        it[FichasTable.tecido], it[FichasTable.cor], it[FichasTable.p],
                        it[FichasTable.m], it[FichasTable.g], it[FichasTable.gg],
                        it[FichasTable.folhas], it[FichasTable.metragem], it[FichasTable.observacoes],
                        it[FichasTable.status]
                    )
                }
            }
            call.respond(fichas)
        }

        get("/") {
            val fichas = transaction {
                FichasTable.selectAll().map {
                    FichaCorte(
                        it[FichasTable.id], it[FichasTable.referencia], it[FichasTable.cliente],
                        it[FichasTable.tecido], it[FichasTable.cor], it[FichasTable.p],
                        it[FichasTable.m], it[FichasTable.g], it[FichasTable.gg],
                        it[FichasTable.folhas], it[FichasTable.metragem], it[FichasTable.observacoes],
                        it[FichasTable.status]
                    )
                }
            }

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
                                fichas.forEach { ficha ->
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

        get("/imprimir/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            val ficha = transaction {
                FichasTable.selectAll().where { FichasTable.id eq (idParam ?: -1) }.map {
                    FichaCorte(
                        it[FichasTable.id], it[FichasTable.referencia], it[FichasTable.cliente],
                        it[FichasTable.tecido], it[FichasTable.cor], it[FichasTable.p],
                        it[FichasTable.m], it[FichasTable.g], it[FichasTable.gg],
                        it[FichasTable.folhas], it[FichasTable.metragem], it[FichasTable.observacoes],
                        it[FichasTable.status]
                    )
                }.firstOrNull()
            }

            if (ficha == null) {
                call.respondText("Ficha não encontrada", status = HttpStatusCode.NotFound)
                return@get
            }

            call.respondHtml {
                head {
                    title { +"Ficha - ${ficha.referencia}" }
                    style {
                        +"""
                            body { font-family: sans-serif; padding: 40px; }
                            .print-header { border: 2px solid #000; padding: 15px; text-align: center; margin-bottom: 20px; }
                            .grade-box { border-collapse: collapse; width: 100%; margin-top: 20px; }
                            .grade-box th, .grade-box td { border: 1px solid #000; padding: 10px; text-align: center; }
                            @media print { .no-print { display: none; } }
                        """.trimIndent()
                    }
                }
                body {
                    div("no-print") {
                        button { attributes["onclick"] = "window.print()"; +"Imprimir PDF" }
                        a(href = "/") { +" Voltar" }
                        hr()
                    }
                    div("print-header") {
                        h1 { +"FICHA DE CORTE" }
                        p { +"REF: ${ficha.referencia} | Data: ${java.time.LocalDate.now()}" }
                    }
                    p { b { +"Cliente: " }; +ficha.cliente }
                    p { b { +"Tecido: " }; +ficha.tecido; b { +" | Cor: " }; +ficha.cor }
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
                    p { b { +"Observações: " }; +ficha.observacoes }
                }
            }
        }

        post("/update-status/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            val newStatus = call.receiveParameters()["newStatus"]
            if (idParam != null && newStatus != null) {
                transaction {
                    FichasTable.update({ FichasTable.id eq idParam }) {
                        it[status] = newStatus
                    }
                }
            }
            call.respondRedirect("/")
        }

        get("/nova-ficha") {
            call.respondHtml {
                head {
                    title { +"Nova Ficha" }
                    style {
                        +"""
                            body { font-family: sans-serif; background: #f0f2f5; padding: 20px; }
                            .card { max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; }
                            input, textarea { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px; }
                            .btn-save { background: #28a745; color: white; border: none; padding: 15px; width: 100%; cursor: pointer; font-weight: bold; }
                        """.trimIndent()
                    }
                }
                body {
                    div("card") {
                        h2 { +"Nova Ficha de Corte" }
                        form(action = "/save", method = FormMethod.post) {
                            input(type = InputType.text, name = "referencia") { placeholder = "REF"; required = true }
                            input(type = InputType.text, name = "cliente") { placeholder = "Cliente" }
                            input(type = InputType.text, name = "tecido") { placeholder = "Tecido" }
                            input(type = InputType.text, name = "cor") { placeholder = "Cor" }
                            div {
                                style = "display: flex; gap: 10px;"
                                input(type = InputType.number, name = "p") { value = "0"; placeholder = "P" }
                                input(type = InputType.number, name = "m") { value = "0"; placeholder = "M" }
                                input(type = InputType.number, name = "g") { value = "0"; placeholder = "G" }
                                input(type = InputType.number, name = "gg") { value = "0"; placeholder = "GG" }
                            }
                            textArea { name = "observacoes"; placeholder = "Observações" }
                            button(type = ButtonType.submit, classes = "btn-save") { +"SALVAR FICHA" }
                        }
                    }
                }
            }
        }

        post("/save") {
            val p = call.receiveParameters()
            transaction {
                FichasTable.insert {
                    it[referencia] = p["referencia"] ?: ""
                    it[cliente] = p["cliente"] ?: ""
                    it[tecido] = p["tecido"] ?: ""
                    it[cor] = p["cor"] ?: ""
                    it[FichasTable.p] = p["p"]?.toIntOrNull() ?: 0
                    it[FichasTable.m] = p["m"]?.toIntOrNull() ?: 0
                    it[FichasTable.g] = p["g"]?.toIntOrNull() ?: 0
                    it[FichasTable.gg] = p["gg"]?.toIntOrNull() ?: 0
                    it[folhas] = 0
                    it[metragem] = ""
                    it[observacoes] = p["observacoes"] ?: ""
                }
            }
            call.respondRedirect("/")
        }
    }
}
