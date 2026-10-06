package pe.edu.upeu.pharmamobil.data.remote

data class ApiConfig(val baseUrl: String, val loggingEnabled: Boolean = false) {
    init {
        require(baseUrl.startsWith("https://") && baseUrl.endsWith("/")) {
            "La URL base debe usar HTTPS y terminar en /"
        }
    }
}
