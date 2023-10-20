package com.labosu.kmposable


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

fun StoreCoroutineTest.createTestStore(
    initialState: TestState = TestState(),
    reducer: Reducer<TestState, TestAction> = TestReducer(),
    defaultExceptionHandler: ExceptionHandler = TestStoreExceptionHandler(),
) = createStore(
    initialState, reducer, defaultExceptionHandler,
    storeScope = this.testCoroutineScope,
    storeDispatcher = this.testDispatcher,
    effectDispatcher = this.testDispatcher
)

data class TestState(val testProperty: String = "")

sealed class TestAction {
    data class ChangeTestProperty(val testProperty: String) : TestAction()
    data class AddToTestProperty(val testPropertySuffix: String) : TestAction()
    data class StartEffectAction(val effect: Effect<TestAction>) : TestAction()
    data class LongRunningScopedEffectAction(override val scope: CoroutineScope) : TestAction(), ScopedAction
    data object ClearTestPropertyFromEffect : TestAction()
    data object DoNothingAction : TestAction()
    data object StartExceptionThrowingEffectAction : TestAction()
    data object DoNothingFromEffectAction : TestAction()
    data object ThrowExceptionAction : TestAction()
}

class TestStoreExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean {
        println("TestStoreExceptionHandler.handleException: ${exception.message}")
        return false
    }
}

class TestReducer : Reducer<TestState, TestAction> {
    var reduceCount: Int = 0
    val actions = mutableListOf<TestAction>()

    override fun reduce(state: Mutable<TestState>, action: TestAction): Effect<TestAction> {
        reduceCount++
        actions.add(action)

        return when (action) {
            is TestAction.ChangeTestProperty -> state.mutateWithoutEffects {
                copy(testProperty = action.testProperty)
            }

            is TestAction.AddToTestProperty -> state.mutateWithoutEffects {
                copy(testProperty = testProperty + action.testPropertySuffix)
            }

            TestAction.ClearTestPropertyFromEffect -> state.mutateWithoutEffects {
                copy(testProperty = "")
            }

            is TestAction.StartEffectAction -> action.effect
            TestAction.DoNothingAction -> noEffect()
            TestAction.DoNothingFromEffectAction -> noEffect()
            TestAction.ThrowExceptionAction -> throw Exception("ThrowExceptionAction")
            TestAction.StartExceptionThrowingEffectAction -> Effect {
                flow {
                    throw IllegalStateException("THIS SHOULD THROW")//throw an exception
                }
            }

            is TestAction.LongRunningScopedEffectAction -> Effect {
                flow {
                    var cnt = 1
                    while (true) {
                        emit(TestAction.ChangeTestProperty(cnt.toString()))
                        cnt++
                        delay(50)
                    }
                }
            }
        }
    }
}