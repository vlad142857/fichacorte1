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

// Tabela do Banco de Dados
object FichasTable : Table("fichas") {
    val id = integer("id").autoIncrement()
    val referencia = varchar("referencia", 50)
    val cliente = varchar("cliente", 100)
    val tecido = varchar("tecido", 100)
    val cor = varchar("cor", 50)
    val pSize = integer("p")
    val mSize = integer("m")
    val gSize = integer("g")
    val ggSize = integer("gg")
    val status = varchar("status", 20).default("No Corte")
    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class FichaCorte(
    val id: Int, val referencia: String, val cliente: String,
    val tecido: String, val cor: String, val p: Int, val m: Int,
    val g: Int, val gg: Int, var status: String
)

fun main() {
    Database.connect("jdbc:h2:file:./fichacorte_db;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    transaction {
        SchemaUtils.create(FichasTable)
    }

    // Voltando para a porta 8080 que é o padrão
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    routing {
        get("/api/fichas") {
            val fichas = transaction {
                FichasTable.selectAll().map {
                    FichaCorte(it[FichasTable.id], it[FichasTable.referencia], it[FichasTable.cliente],
                        it[FichasTable.tecido], it[FichasTable.cor], it[FichasTable.pSize],
                        it[FichasTable.mSize], it[FichasTable.gSize], it[FichasTable.ggSize], it[FichasTable.status])
                }
            }
            call.respond(fichas)
        }

        get("/") {
            val fichas = transaction {
                FichasTable.selectAll().map {
                    FichaCorte(it[FichasTable.id], it[FichasTable.referencia], it[FichasTable.cliente],
                        it[FichasTable.tecido], it[FichasTable.cor], it[FichasTable.pSize],
                        it[FichasTable.mSize], it[FichasTable.gSize], it[FichasTable.ggSize], it[FichasTable.status])
                }
            }

            call.respondHtml {
                head {
                    title { +"Controle de Produção" }
                    style { 
                        unsafe { 
                            +"""
                                body { font-family: sans-serif; background: #f4f7f6; margin: 0; padding: 20px; }
                                .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
                                table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                                th, td { padding: 12px; border-bottom: 1px solid #eee; text-align: left; }
                                .status { padding: 5px 10px; border-radius: 4px; font-weight: bold; font-size: 12px; }
                                .status-corte { background: #fff3cd; color: #856404; }
                                .status-costura { background: #cce5ff; color: #004085; }
                                .btn { background: #007bff; color: white; padding: 8px 15px; text-decoration: none; border-radius: 4px; display: inline-block; }
                            """.trimIndent()
                        }
                    }
                }
                body {
                    div("card") {
                        h2 { +"📋 Fichas de Corte em Produção" }
                        a(href = "/nova-ficha", classes = "btn") { +"+ Nova Ficha" }
                        table {
                            tr { th { +"REF" }; th { +"Cliente" }; th { +"Produto" }; th { +"Status" }; th { +"Ação" } }
                            fichas.forEach { ficha ->
                                tr {
                                    td { +ficha.referencia }
                                    td { +ficha.cliente }
                                    td { +"${ficha.tecido} (${ficha.cor})" }
                                    td { span("status status-${if(ficha.status == "No Corte") "corte" else "costura"}") { +ficha.status } }
                                    td {
                                        if (ficha.status == "No Corte") {
                                            form(action = "/status/${ficha.id}", method = FormMethod.post) {
                                                button { +"Enviar p/ Costura" }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        get("/nova-ficha") {
            call.respondHtml {
                head {
                    style { unsafe { +".form-card { max-width: 400px; margin: auto; padding: 20px; border: 1px solid #ccc; border-radius: 8px; }" } }
                }
                body {
                    div("form-card") {
                        h2 { +"Nova Ficha de Corte" }
                        form(action = "/save", method = FormMethod.post) {
                            label { +"Referência: " }; input(type = InputType.text, name = "ref"); br()
                            label { +"Cliente: " }; input(type = InputType.text, name = "cli"); br()
                            label { +"Tecido: " }; input(type = InputType.text, name = "tec"); br()
                            label { +"Cor: " }; input(type = InputType.text, name = "cor"); br()
                            button(type = ButtonType.submit) { +"Salvar Ficha" }
                        }
                        br(); a(href = "/") { +"Voltar" }
                    }
                }
            }
        }

        post("/save") {
            val params = call.receiveParameters()
            transaction {
                FichasTable.insert {
                    it[referencia] = params["ref"] ?: ""
                    it[cliente] = params["cli"] ?: ""
                    it[tecido] = params["tec"] ?: ""
                    it[cor] = params["cor"] ?: ""
                    it[pSize] = 0; it[mSize] = 0; it[gSize] = 0; it[ggSize] = 0
                }
            }
            call.respondRedirect("/")
        }

        post("/status/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            if (idParam != null) {
                transaction {
                    FichasTable.update({ FichasTable.id eq idParam }) {
                        it[status] = "Na Costura"
                    }
                }
            }
            call.respondRedirect("/")
        }
    }
}
