package com.fkarach.arquibancada.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Agenda(
    val versao: Int = 1,
    @SerialName("atualizado_em") val atualizadoEm: String = "",
    @SerialName("proxima_atualizacao") val proximaAtualizacao: String? = null,
    val eventos: List<Evento> = emptyList(),
)

@Serializable
data class Evento(
    val id: String,
    val esporte: String,
    val modalidade: String? = null,
    val competicao: String = "",
    val titulo: String,
    val monogramas: List<String> = emptyList(),
    val inicio: String,
    @SerialName("horario_definido") val horarioDefinido: Boolean = true,
    val situacao: String = "agendado",
    @SerialName("inicio_anterior") val inicioAnterior: String? = null,
    val local: String? = null,
    @SerialName("onde_assistir") val ondeAssistir: List<Onde> = emptyList(),
    @SerialName("status_canal") val statusCanal: String = "desconhecido",
    @SerialName("origem_canal") val origemCanal: String? = null,
    @SerialName("meu_time") val meuTime: Boolean = false,
    val pontos: Int = 0,
    val motivos: List<String> = emptyList(),
    @SerialName("por_que_vale") val porQueVale: List<String> = emptyList(),
    val observacao: String? = null,
)

@Serializable
data class Onde(
    val canal: String,
    val tipo: String = "",
    val gratis: Boolean = false,
    val servico: String? = null,
)

@Serializable
data class Seguindo(
    val versao: Int = 1,
    val seguindo: List<Seguido> = emptyList(),
    val modalidades: List<Modalidade> = emptyList(),
    val servicos: List<String> = emptyList(),
)

@Serializable
data class Seguido(val nome: String, val esporte: String, val nivel: String)

@Serializable
data class Modalidade(val esporte: String, val modalidade: String? = null, val nivel: String = "normal")
