import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainWindowBloc {

    private val mainScope = MainScope()

    /*
    private val _effects = Channel<MainWindowEffect>(Channel.UNLIMITED)
    val effects = _effects.consumeAsFlow()
    */

    private val _state = MutableStateFlow(MainWindowState.INITIAL)
    val state = _state.asStateFlow()

    @ObsoleteCoroutinesApi
    private val eventSink = mainScope.actor<MainWindowEvent> {
        for (event in channel) {
            // mapEventToEffect(event)?.also { effect -> _effects.send(effect) }
            mapEventToState(event, _state.value).also { state -> _state.value = state }
        }
    }

    @ObsoleteCoroutinesApi
    fun addEvent(event: MainWindowEvent) {
        mainScope.launch { eventSink.send(event) }
    }

    /*
    private fun mapEventToEffect(event: MainWindowEvent): MainWindowEffect? =
        when (event) {
            ConvertButtonClicked ->
                ShowSnackbar("Can't do that right now", "Wait", SnackbarDuration.Short)
            else -> null
        }
    */

    private fun mapEventToState(event: MainWindowEvent, currentState: MainWindowState) =
        when (event) {
            is SourceFilesSelected ->
                currentState.copy(
                    sourceFilesSelectionTextFieldState = TextFieldState(
                        value = event.files.joinToString(", "),
                        isErrorValue = event.files.any { !it.exists() }
                    )
                )
            is DestinationDirectorySelected ->
                currentState.copy(
                    destinationDirectorySelectionTextFieldState = TextFieldState(
                        value = event.directory.path.orEmpty(),
                        isErrorValue = !event.directory.exists()
                    )
                )
            is AllInOneCheckboxClicked ->
                currentState.copy(
                    isAllInOneCheckboxChecked = event.isChecked
                )
            is ConvertButtonClicked -> TODO()
        }
}
