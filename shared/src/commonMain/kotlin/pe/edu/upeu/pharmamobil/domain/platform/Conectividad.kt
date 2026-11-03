package pe.edu.upeu.pharmamobil.domain.platform

interface Conectividad { fun observar(): kotlinx.coroutines.flow.Flow<Boolean> }
interface AvisosSincronizacion { fun completada() }
