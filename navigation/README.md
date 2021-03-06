# Navigation

Gives an universal way to navigate between screens.

## What problems does it solve?
Organizing navigation in Android applications is not a straightforward task. There are difficulties that are common for most applications:

1. **Calling navigation from View Models is not simple.**
   View Models are good classes to place navigation logic. Activity and FragmentManager are required to implement navigation calls, but it is a bad idea to reference them from View Models. It causes memory leaks and undesired dependencies.
   
2. **Fragment transactions can cause crashes.**
   There are limitations when a fragment transaction can be executed.
   Calling `commit` after `onSaveInstanceState` causes `IllegalStateException: "Can not perform this action after onSaveInstanceState"`. Committing a transaction when the other one is not finished yet causes another exception `IllegalStateException: "FragmentManager is already executing transactions"`.
   It is not simple to control that transactions are executed at a correct time because often navigation is required in consequence of some asynchronous operation.

3. **Nested navigation is hard to implement.**
   Navigation can be nested. Navigation calls change screens on different hierarchy levels. View Models ideally should not be aware of this logic.
   
## How does it work?
Instead of calling navigation methods directly Sesame provides navigation messages - a marker interface `NavigationMessage`. View Model sends navigation messages by enqueuing them to `NavigationMessageQueue`. On the Activity/Fragment side navigation messages are passed to `NavigationMessageDispatcher`. The dispatcher handles messages, but it doesn't do it by itself. It delegates this task to `NavigationMessageHandler`s. A `NavigationMessageHandler` is responsible for navigation implementation (starting activities or committing fragment transactions). There could be several navigation message handlers. In that case they work like a chain - if some handler doesn't handle a message then this message is passed further.

## How does it solve the problems?

1. `NavigationMessage`s and `NavigationMessageQueue` allow to call navigation from View Models.

2. It is guaranteed that fragment transactions are commited when it is legal. `NavigationMessageQueue` passes messages only when the corresponding Activity/Fragment is in resumed state. `NavigationMessageDispatcher` has an internal queue to handle messages sequentially.

3. Nested navigation is solved by several `NavigationMessageHandler`s. The typical Android hierarchy is [Fragment] -> [parent Fragment] -> [Activity]. The parent Fragment and Activity implement `NavigationMessageHandler`, so different messages are handled on the different levels.

## How to use?

1. Declare navigation messages.
```kotlin
object Back : NavigationMessage
object OpenProfileScreen : NavigationMessage
```

2. Use `NavigationMessageQueue` in View Model to send messages.
```kotlin
class MenuViewModel : ViewModel() {

    val navigationMessageQueue = NavigationMessageQueue()

    fun onBackPressed() {
        navigationMessageQueue.send(Back)
    }

    fun onProfileButtonClicked() {
        navigationMessageQueue.send(OpenProfileScreen)
    }
}
```

3. Setup `NavigationMessageDispatcher` in an activity and fragments. This has to be the same instance.
```kotlin
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var navigationMessageDispatcher: NavigationMessageDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        navigationMessageDispatcher.attach(this)
        ...
    }
}
```

```kotlin
class MenuFragment: Fragment() {

    @Inject
    internal lateinit var navigationMessageDispatcher: NavigationMessageDispatcher

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        vm.navigationMessageQueue.bind(navigationMessageDispatcher, node = this, viewLifecycleOwner)
        ...
    }
}
```

4. Implement `NavigationMessageHandler`. Returned `true` from `handleNavigationMessage` indicates that a message was handled. If there is only one `NavigationMessageHandler` all the messages should be handled there. The example uses [FragmentNavigator](https://github.com/aartikov/Sesame/blob/master/sample/src/main/kotlin/me/aartikov/sesamesample/FragmentNavigator.kt) - a wrapper on top of FragmentManager, but `NavController` from Android Architecture Components can be used as well.
```kotlin
class MainActivity : AppCompatActivity(), NavigationMessageHandler {
    
    ...
    
    override fun handleNavigationMessage(message: NavigationMessage): Boolean {
        when (message) {
            is Back -> {
                val success = navigator.back()
                if (!success) finish()
            }
            is OpenProfileScreen -> navigator.goTo(ProfileFragment())
        }
        return true
    }
}
```