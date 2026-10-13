package pe.edu.upeu.pharmamobil.data.remote

data class ApiConfig(val baseUrl: String, val loggingEnabled: Boolean = false) {
    init {
        require((baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) && baseUrl.endsWith("/")) {
            "La URL base debe usar HTTP/HTTPS y terminar en /"
        }
    }
}
