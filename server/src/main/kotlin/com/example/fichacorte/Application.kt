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

// Definição da Tabela
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
    // jdbc:h2:file: garante que salve em arquivo no disco local
    Database.connect("jdbc:h2:file:./fichacorte_db;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    
    transaction {
        addLogger(StdOutSqlLogger) // ISSO VAI MOSTRAR O SQL NO CONSOLE
        SchemaUtils.create(FichasTable)
        println("Banco de dados iniciado e tabela verificada.")
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
        // API para o Celular
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

        // Listagem no Site
        get("/") {
            val fichas = transaction {
                addLogger(StdOutSqlLogger)
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
                                if (fichas.isEmpty()) {
                                    tr { td { attributes["colspan"] = "5"; +"Nenhuma ficha cadastrada no banco." } }
                                }
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

        // Salvar Nova Ficha
        post("/save") {
            val p = call.receiveParameters()
            println("Recebendo dados para salvar: ${p["referencia"]}")
            
            try {
                transaction {
                    addLogger(StdOutSqlLogger)
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
                println("Ficha salva com sucesso no banco!")
            } catch (e: Exception) {
                println("ERRO AO SALVAR NO BANCO: ${e.message}")
                e.printStackTrace()
            }
            
            call.respondRedirect("/")
        }

        // Outras rotas (Imprimir, Update Status, Nova Ficha) permanecem iguais...
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
            } else {
                call.respondHtml {
                    body { h1 { +"Imprimindo ${ficha.referencia}" }; p { +"Ficha técnica aqui..." } }
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
                body {
                    h2 { +"Nova Ficha" }
                    form(action = "/save", method = FormMethod.post) {
                        input(type = InputType.text, name = "referencia") { placeholder = "REF" }
                        br()
                        button(type = ButtonType.submit) { +"SALVAR" }
                    }
                }
            }
        }
    }
}
