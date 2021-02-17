package me.aartikov.lib.navigation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidNavigationMessageDispatcher(
    private val nodeWalker: NodeWalker = AndroidNodeWalker(),
    private val errorHandler: ((Exception) -> Unit)? = null
) : NavigationMessageDispatcher {

    private var messageChannel = Channel<Pair<NavigationMessage, Any>>(Channel.UNLIMITED)
    private var collectingJob: Job? = null

    override fun attach(lifecycleOwner: LifecycleOwner) {
        if (collectingJob != null) {
            collectingJob?.cancel()

            // drop old messages, because firstNodes for them belong to an old activity
            messageChannel.close()
            messageChannel = Channel(Channel.UNLIMITED)
        }

        collectingJob = lifecycleOwner.lifecycleScope.launchWhenResumed {
            messageChannel.receiveAsFlow().collect { (message, firstNode) ->
                process(message, firstNode)
            }
        }
    }

    override fun dispatch(message: NavigationMessage, firstNode: Any) {
        messageChannel.offer(Pair(message, firstNode))
    }

    private fun process(message: NavigationMessage, firstNode: Any) {
        var node: Any? = firstNode
        do {
            if (node is NavigationMessageHandler && node.handleNavigationMessage(message)) {
                return
            }
            node = node?.let { nodeWalker.getNextNode(it) }
        } while (node != null)

        if (errorHandler != null) {
            errorHandler.invoke(NotHandledNavigationMessageException())
        } else {
            throw NotHandledNavigationMessageException()
        }
    }
}