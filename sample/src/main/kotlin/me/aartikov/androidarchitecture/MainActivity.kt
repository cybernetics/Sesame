package me.aartikov.androidarchitecture

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import me.aartikov.androidarchitecture.counter.CounterScreen
import me.aartikov.androidarchitecture.dialogs.ClockScreen
import me.aartikov.androidarchitecture.dialogs.DialogsScreen
import me.aartikov.androidarchitecture.menu.MenuScreen
import me.aartikov.androidarchitecture.movies.ui.MoviesScreen
import me.aartikov.androidarchitecture.profile.ui.ProfileScreen
import me.aartikov.lib.navigation.NavigationMessage
import me.aartikov.lib.navigation.NavigationMessageDispatcher
import me.aartikov.lib.navigation.NavigationMessageHandler
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationMessageHandler {

    @Inject
    internal lateinit var navigationMessageDispatcher: NavigationMessageDispatcher

    private lateinit var navigator: FragmentNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationMessageDispatcher.attach(this)
        navigator = FragmentNavigator(R.id.container, supportFragmentManager)

        if (savedInstanceState == null) {
            navigator.setRoot(MenuScreen())
        }
    }

    override fun handleNavigationMessage(message: NavigationMessage): Boolean {
        when (message) {
            is Back -> navigator.back()
            is OpenCounterScreen -> navigator.goTo(CounterScreen())
            is OpenProfileScreen -> navigator.goTo(ProfileScreen())
            is OpenDialogsScreen -> navigator.goTo(DialogsScreen())
            is OpenMoviesScreen -> navigator.goTo(MoviesScreen())
            is OpenClockScreen -> navigator.goTo(ClockScreen())
        }
        return true
    }

    override fun onBackPressed() {
        if (!navigator.back()) finish()
    }
}