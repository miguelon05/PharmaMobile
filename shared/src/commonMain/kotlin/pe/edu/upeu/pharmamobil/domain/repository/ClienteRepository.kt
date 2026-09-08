package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Cliente

interface ClienteRepository {


    suspend fun registrar(cliente: Cliente): Cliente


    suspend fun listar(): List<Cliente>
}
