package me.aartikov.lib.loading.simple.internal

import me.aartikov.lib.loading.simple.Loading.Event
import me.aartikov.lib.loading.simple.Loading.State
import me.aartikov.lib.state_machine.*

internal sealed class Action<out T> {
    data class Load(val fresh: Boolean) : Action<Nothing>()
    object Refresh : Action<Nothing>()

    data class DataLoaded<T>(val data: T) : Action<T>()
    object EmptyDataLoaded : Action<Nothing>()
    data class LoadingError(val throwable: Throwable) : Action<Nothing>()

    data class DataObserved<T>(val data: T) : Action<T>()
    object EmptyDataObserved : Action<Nothing>()
}

internal sealed class Effect {
    data class Load(val fresh: Boolean) : Effect()
    object Refresh : Effect()
    data class EmitEvent(val event: Event) : Effect()
}

internal typealias LoadingStateMachine<T> = StateMachine<State<T>, Action<T>, Effect>

internal class LoadingReducer<T> : Reducer<State<T>, Action<T>, Effect> {

    override fun reduce(state: State<T>, action: Action<T>): Next<State<T>, Effect> = when (action) {

        is Action.Load -> {
            when (state) {
                is State.Empty -> next(
                    State.EmptyLoading,
                    Effect.Load(action.fresh)
                )
                else -> nothing()
            }
        }

        is Action.Refresh -> {
            when (state) {
                is State.Empty -> next(
                    State.EmptyLoading,
                    Effect.Refresh
                )
                is State.EmptyError -> next(
                    State.EmptyLoading,
                    Effect.Refresh
                )
                is State.Data -> next(
                    State.Refresh(data = state.data),
                    Effect.Refresh
                )
                else -> nothing()
            }
        }

        is Action.DataLoaded -> {
            when (state) {
                is State.EmptyLoading -> next(State.Data(action.data))
                is State.Refresh -> next(State.Data(action.data))
                else -> nothing()
            }
        }

        is Action.EmptyDataLoaded -> {
            when (state) {
                is State.EmptyLoading -> next(State.Empty)
                is State.Refresh -> next(State.Empty)
                else -> nothing()
            }
        }

        is Action.LoadingError -> {
            when (state) {
                is State.EmptyLoading -> next(
                    State.EmptyError(action.throwable),
                    Effect.EmitEvent(Event.Error(action.throwable, hasData = false))
                )
                is State.Refresh -> next(
                    State.Data(state.data),
                    Effect.EmitEvent(Event.Error(action.throwable, hasData = true))
                )
                else -> nothing()
            }
        }

        is Action.DataObserved -> {
            when (state) {
                is State.Empty -> next(State.Data(action.data))
                is State.EmptyLoading -> next(State.Refresh(action.data))
                is State.EmptyError -> next(State.Data(action.data))    // TODO: Effect.EmitEvent(Event.Error(action.throwable, hasData = true)) ???
                is State.Refresh -> next(State.Refresh(action.data))
                is State.Data -> next(State.Data(action.data))
                else -> nothing()
            }
        }

        is Action.EmptyDataObserved -> {
            when (state) {
                is State.Refresh -> next(State.EmptyLoading)
                is State.Data -> next(State.Empty)
                else -> nothing()
            }
        }
    }
}