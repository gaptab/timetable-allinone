package danielabbott.personalorganiser.ui

interface OnBackPressed {
    // This is called when the back button is pressed (hardware or android UI) and when a main menu
    // button is pressed as this also closes the page
    fun onBackPressed(onNoChangesOrDiscardChanges: () -> Unit)
}
