package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioActualDto

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto): LoginResponseDto =
        client.post("auth/login") {
            setBody(request)
        }.body()

    suspend fun me(): UsuarioActualDto =
        client.get("auth/me").body()
}
