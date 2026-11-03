package pe.edu.upeu.pharmamobil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity(), org.koin.core.component.KoinComponent {
    private val permisos get() = getKoin().get<pe.edu.upeu.pharmamobil.data.permiso.GestorPermisosAndroid>()
    private lateinit var puentePermisos: pe.edu.upeu.pharmamobil.data.permiso.PuentePermisos
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        puentePermisos = pe.edu.upeu.pharmamobil.data.permiso.PuentePermisos(this)
        permisos.puente = puentePermisos
        setContent {
            App()
        }
    }
    override fun onDestroy() {
        if (permisos.puente === puentePermisos) permisos.puente = null
        puentePermisos.cerrar()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}