import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.ExperimentalLayout
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalLayout
@ExperimentalMaterialApi
@ObsoleteCoroutinesApi
fun main() = Window(
    title = "svg2iv",
    size = IntSize(800, 350),
    resizable = false
) {
    MainWindowContent(MainWindowBloc())
}
