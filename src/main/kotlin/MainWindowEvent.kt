import java.io.File

sealed class MainWindowEvent

class SourceFilesSelected(val files: Array<File>) : MainWindowEvent()

class DestinationDirectorySelected(val directory: File) : MainWindowEvent()

class AllInOneCheckboxClicked(val isChecked: Boolean) : MainWindowEvent()

object ConvertButtonClicked : MainWindowEvent()
