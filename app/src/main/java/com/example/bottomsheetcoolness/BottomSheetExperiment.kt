//package com.example.bottomsheetcoolness
//
//import androidx.compose.animation.core.AnimationSpec
//import androidx.compose.animation.core.TweenSpec
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.Orientation
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.SaveableStateHolder
//import androidx.compose.runtime.saveable.Saver
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.runtime.saveable.rememberSaveableStateHolder
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Shape
//import androidx.compose.ui.graphics.isSpecified
//import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
//import androidx.compose.ui.input.nestedscroll.NestedScrollSource
//import androidx.compose.ui.input.nestedscroll.nestedScroll
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.semantics.*
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.Velocity
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.Lifecycle
//import androidx.navigation.*
//import androidx.navigation.compose.LocalOwnersProvider
//import com.example.bottomsheetcoolness.TogglBottomSheetNavigator.Destination
//import com.example.bottomsheetcoolness.TogglModalBottomSheetValue.*
//import com.google.accompanist.insets.ExperimentalAnimatedInsets
//import com.google.accompanist.insets.rememberImeNestedScrollConnection
//import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
//import com.google.accompanist.navigation.material.ModalBottomSheetLayout
//import kotlinx.coroutines.CancellationException
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withTimeout
//import kotlin.math.max
//import kotlin.math.roundToInt
//
//@ExperimentalMaterialNavigationApi
//@OptIn(ExperimentalMaterialApi::class)
//public class TogglBottomSheetNavigatorSheetState(private val sheetState: TogglModalBottomSheetState) {
//    /**
//     * @see ModalBottomSheetState.isVisible
//     */
//    public val isVisible: Boolean
//        get() = sheetState.isVisible
//
//    /**
//     * @see ModalBottomSheetState.currentValue
//     */
//    public val currentValue: TogglModalBottomSheetValue
//        get() = sheetState.currentValue
//
//    /**
//     * @see ModalBottomSheetState.targetValue
//     */
//    public val targetValue: TogglModalBottomSheetValue
//        get() = sheetState.targetValue
//
//    /**
//     * @see ModalBottomSheetState.offset
//     */
//    public val offset: State<Float>
//        get() = sheetState.offset
//
//    /**
//     * @see ModalBottomSheetState.overflow
//     */
//    public val overflow: State<Float>
//        get() = sheetState.overflow
//
//    /**
//     * @see ModalBottomSheetState.direction
//     */
//    public val direction: Float
//        get() = sheetState.direction
//
//    /**
//     * @see ModalBottomSheetState.progress
//     */
//    public val progress: SwipeProgress<TogglModalBottomSheetValue>
//        get() = sheetState.progress
//}
//
///**
// * Create and remember a [TogglBottomSheetNavigator]
// */
//@ExperimentalMaterialNavigationApi
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//public fun rememberTogglBottomSheetNavigator(
//    peekHeight: Dp = 100.dp,
//    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec
//): TogglBottomSheetNavigator {
//    val sheetState = rememberTogglModalBottomSheetState(
//        Hidden,
//        peekHeight,
//        animationSpec
//    )
//    return remember(sheetState) {
//        TogglBottomSheetNavigator(sheetState = sheetState)
//    }
//}
//
///**
// * Navigator that drives a [ModalBottomSheetState] for use of [ModalBottomSheetLayout]s
// * with the navigation library. Every destination using this Navigator must set a valid
// * [Composable] by setting it directly on an instantiated [Destination] or calling
// * [androidx.navigation.compose.material.bottomSheet].
// *
// * <b>The [sheetContent] [Composable] will always host the latest entry of the back stack. When
// * navigating from a [TogglBottomSheetNavigator.Destination] to another
// * [TogglBottomSheetNavigator.Destination], the content of the sheet will be replaced instead of a
// * new bottom sheet being shown.</b>
// *
// * When the sheet is dismissed by the user, the [state]'s [NavigatorState.backStack] will be popped.
// *
// * @param sheetState The [ModalBottomSheetState] that the [TogglBottomSheetNavigator] will use to
// * drive the sheet state
// */
//@ExperimentalMaterialNavigationApi
//@OptIn(ExperimentalMaterialApi::class)
//@Navigator.Name("BottomSheetNavigator")
//public class TogglBottomSheetNavigator(
//    internal val sheetState: TogglModalBottomSheetState
//) : Navigator<TogglBottomSheetNavigator.Destination>() {
//
//    private var attached by mutableStateOf(false)
//
//    /**
//     * Get the back stack from the [state]. In some cases, the [sheetContent] might be composed
//     * before the Navigator is attached, so we specifically return an empty flow if we aren't
//     * attached yet.
//     */
//    private val backStack: StateFlow<List<NavBackStackEntry>>
//        get() = if (attached) {
//            state.backStack
//        } else {
//            MutableStateFlow(emptyList())
//        }
//
//    /**
//     * Access properties of the [ModalBottomSheetLayout]'s [ModalBottomSheetState]
//     */
//    public val navigatorSheetState = TogglBottomSheetNavigatorSheetState(sheetState)
//
//    /**
//     * A [Composable] function that hosts the current sheet content. This should be set as
//     * sheetContent of your [ModalBottomSheetLayout].
//     */
//    public val sheetContent: @Composable ColumnScope.() -> Unit = @Composable {
//        val columnScope = this
//        val saveableStateHolder = rememberSaveableStateHolder()
//        val backStackEntries by backStack.collectAsState()
//
//        // We always replace the sheet's content instead of overlaying and nesting floating
//        // window destinations. That means that only *one* concurrent destination is supported by
//        // this navigator.
//        val latestEntry = backStackEntries.lastOrNull { entry ->
//            // We might have entries in the back stack that aren't started currently, so filter
//            // these
//            entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
//        }
//
//        // Mark all of the entries' transitions as complete, except for the entry we are
//        // currently displaying because it will have its transition completed when the sheet's
//        // animation has completed
//        DisposableEffect(backStackEntries) {
//            backStackEntries.forEach {
//                if (it != latestEntry) state.markTransitionComplete(it)
//            }
//            onDispose { }
//        }
//
//        TogglSheetContentHost(
//            columnHost = columnScope,
//            backStackEntry = latestEntry,
//            sheetState = sheetState,
//            saveableStateHolder = saveableStateHolder,
//            onSheetShown = { backStackEntry ->
//                state.markTransitionComplete(backStackEntry)
//            },
//            onSheetDismissed = { backStackEntry ->
//                // Sheet dismissal can be started through popBackStack in which case we have a
//                // transition that we'll want to complete
//                if (state.transitionsInProgress.value.contains(backStackEntry)) {
//                    state.markTransitionComplete(backStackEntry)
//                } else {
//                    state.pop(popUpTo = backStackEntry, saveState = false)
//                }
//            }
//        )
//    }
//
//    override fun onAttach(state: NavigatorState) {
//        super.onAttach(state)
//        attached = true
//    }
//
//    override fun createDestination(): Destination = Destination(navigatorToggl = this, content = {})
//
//    override fun navigate(
//        entries: List<NavBackStackEntry>,
//        navOptions: NavOptions?,
//        navigatorExtras: Extras?
//    ) {
//        entries.forEach { entry ->
//            state.pushWithTransition(entry)
//        }
//    }
//
//    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
//        state.popWithTransition(popUpTo, savedState)
//    }
//
//    /**
//     * [NavDestination] specific to [TogglBottomSheetNavigator]
//     */
//    @NavDestination.ClassType(Composable::class)
//    public class Destination(
//        navigatorToggl: TogglBottomSheetNavigator,
//        internal val content: @Composable ColumnScope.(NavBackStackEntry) -> Unit
//    ) : NavDestination(navigatorToggl), FloatingWindow
//}
//
//@ExperimentalAnimatedInsets
//@ExperimentalMaterialApi
//@ExperimentalMaterialNavigationApi
//@Composable
//public fun TogglModalBottomSheetLayout(
//    bottomSheetNavigator: TogglBottomSheetNavigator,
//    modifier: Modifier = Modifier,
//    sheetShape: Shape = MaterialTheme.shapes.large,
//    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
//    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
//    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
//    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
//    content: @Composable () -> Unit
//) {
//    TogglModalBottomSheetLayout(
//        togglSheetState = bottomSheetNavigator.sheetState,
//        sheetContent = bottomSheetNavigator.sheetContent,
//        modifier = modifier,
//        sheetShape = sheetShape,
//        sheetElevation = sheetElevation,
//        sheetBackgroundColor = sheetBackgroundColor,
//        sheetContentColor = sheetContentColor,
//        scrimColor = scrimColor,
//        content = content
//    )
//}
//
//@ExperimentalMaterialApi
//enum class TogglModalBottomSheetValue {
//    /**
//     * The bottom sheet is not visible.
//     */
//    Hidden,
//
//    /**
//     * The bottom sheet is visible at full height.
//     */
//    Expanded,
//
//    /**
//     * The bottom sheet is partially visible at 50% of the screen height. This state is only
//     * enabled if the height of the bottom sheet is more than 50% of the screen height.
//     */
//    HalfExpanded
//}
//
///**
// * State of the [TogglModalBottomSheetLayout] composable.
// *
// * @param initialValueToggl The initial value of the state.
// * @param animationSpec The default animation that will be used to animate to a new state.
// * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
// */
//@ExperimentalMaterialApi
//class TogglModalBottomSheetState(
//    initialValue: TogglModalBottomSheetValue,
//    val peekHeight: Dp,
//    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
//    val confirmStateChange: (TogglModalBottomSheetValue) -> Boolean = { true }
//) : SwipeableState<TogglModalBottomSheetValue>(
//    initialValue = initialValue,
//    animationSpec = animationSpec,
//    confirmStateChange = confirmStateChange
//) {
//    /**
//     * Whether the bottom sheet is visible.
//     */
//    val isVisible: Boolean
//        get() = currentValue != Hidden
//
//    /**
//     * Show the bottom sheet with animation and suspend until it's shown. If half expand is
//     * enabled, the bottom sheet will be half expanded. Otherwise it will be fully expanded.
//     *
//     * @throws [CancellationException] if the animation is interrupted
//     */
//    suspend fun show() {
//        animateTo(targetValue = HalfExpanded)
//    }
//
//    /**
//     * Half expand the bottom sheet if half expand is enabled with animation and suspend until it
//     * animation is complete or cancelled
//     *
//     * @throws [CancellationException] if the animation is interrupted
//     */
//    internal suspend fun halfExpand() {
//        animateTo(HalfExpanded)
//    }
//
//    /**
//     * Fully expand the bottom sheet with animation and suspend until it if fully expanded or
//     * animation has been cancelled.
//     * *
//     * @throws [CancellationException] if the animation is interrupted
//     */
//    internal suspend fun expand() = animateTo(Expanded)
//
//    /**
//     * Hide the bottom sheet with animation and suspend until it if fully hidden or animation has
//     * been cancelled.
//     *
//     * @throws [CancellationException] if the animation is interrupted
//     */
//    suspend fun hide() = animateTo(Hidden)
//
//    internal val nestedScrollConnection = this.PreUpPostDownNestedScrollConnection
//
//    companion object {
//        /**
//         * The default [Saver] implementation for [TogglModalBottomSheetState].
//         */
//        fun Saver(
//            animationSpec: AnimationSpec<Float>,
//            peekHeight: Dp,
//            confirmStateChange: (TogglModalBottomSheetValue) -> Boolean
//        ): Saver<TogglModalBottomSheetState, *> = Saver(
//            save = { it.currentValue },
//            restore = {
//                TogglModalBottomSheetState(
//                    initialValue = it,
//                    animationSpec = animationSpec,
//                    peekHeight = peekHeight,
//                    confirmStateChange = confirmStateChange
//                )
//            }
//        )
//    }
//}
//
///**
// * Create a [TogglModalBottomSheetState] and [remember] it.
// *
// * @param initialValue The initial value of the state.
// * @param animationSpec The default animation that will be used to animate to a new state.
// * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
// */
//@Composable
//@ExperimentalMaterialApi
//fun rememberTogglModalBottomSheetState(
//    initialValue: TogglModalBottomSheetValue,
//    peekHeight: Dp,
//    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
//    confirmStateChange: (TogglModalBottomSheetValue) -> Boolean = { true }
//): TogglModalBottomSheetState {
//    return rememberSaveable(
//        saver = TogglModalBottomSheetState.Saver(
//            animationSpec = animationSpec,
//            confirmStateChange = confirmStateChange,
//            peekHeight = peekHeight
//        )
//    ) {
//        TogglModalBottomSheetState(
//            initialValue = initialValue,
//            animationSpec = animationSpec,
//            confirmStateChange = confirmStateChange,
//            peekHeight = peekHeight
//        )
//    }
//}
//
//@ExperimentalAnimatedInsets
//@Composable
//@ExperimentalMaterialApi
//fun TogglModalBottomSheetLayout(
//    sheetContent: @Composable ColumnScope.() -> Unit,
//    modifier: Modifier = Modifier,
//    togglSheetState: TogglModalBottomSheetState =
//        rememberTogglModalBottomSheetState(Hidden, 100.dp),
//    sheetShape: Shape = MaterialTheme.shapes.large,
//    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
//    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
//    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
//    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
//    content: @Composable () -> Unit
//) {
//    val scope = rememberCoroutineScope()
//    BoxWithConstraints(modifier) {
//        val peekHeightInPixels = with(LocalDensity.current) { togglSheetState.peekHeight.toPx() }
//        val fullHeight = constraints.maxHeight.toFloat()
//        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
//        Box(Modifier.fillMaxSize()) {
//            content()
//            Scrim(
//                color = scrimColor,
//                onDismiss = {
//                    if (togglSheetState.confirmStateChange(Hidden)) {
//                        scope.launch { togglSheetState.hide() }
//                    }
//                },
//                visible = togglSheetState.targetValue != Hidden
//            )
//        }
//        Surface(
//            Modifier
//                .fillMaxWidth()
//                .nestedScroll(togglSheetState.nestedScrollConnection)
//                .nestedScroll(rememberImeNestedScrollConnection())
//                .offset {
//                    val y = togglSheetState.offset.value.roundToInt()
//                    IntOffset(0, y)
//                }
//                .onGloballyPositioned {
//                    sheetHeightState.value = it.size.height.toFloat()
//                }
//                .bottomSheetSwipeable(togglSheetState, peekHeightInPixels, fullHeight, sheetHeightState)
//                .semantics {
//                    if (togglSheetState.isVisible) {
//                        dismiss {
//                            if (togglSheetState.confirmStateChange(Hidden)) {
//                                scope.launch { togglSheetState.hide() }
//                            }
//                            true
//                        }
//                        if (togglSheetState.currentValue == HalfExpanded) {
//                            expand {
//                                if (togglSheetState.confirmStateChange(Expanded)) {
//                                    scope.launch { togglSheetState.expand() }
//                                }
//                                true
//                            }
//                        } else {
//                            collapse {
//                                if (togglSheetState.confirmStateChange(HalfExpanded)) {
//                                    scope.launch { togglSheetState.halfExpand() }
//                                }
//                                true
//                            }
//                        }
//                    }
//                },
//            shape = sheetShape,
//            elevation = sheetElevation,
//            color = sheetBackgroundColor,
//            contentColor = sheetContentColor
//        ) {
//            Column(
//                content = sheetContent
//            )
//        }
//    }
//}
//
//@ExperimentalMaterialApi
//@Suppress("ModifierInspectorInfo")
//@Composable
//private fun Modifier.bottomSheetSwipeable(
//    sheetStateToggl: TogglModalBottomSheetState,
//    peekHeight: Float,
//    fullHeight: Float,
//    sheetHeightState: State<Float?>
//): Modifier {
//    val sheetHeight = sheetHeightState.value
//    val modifier = if (sheetHeight != null) {
//        val anchors = mapOf(
//            fullHeight to Hidden,
//            (fullHeight / 2)  to HalfExpanded,
//            max(0f, fullHeight - sheetHeight) to Expanded
//        )
//        Modifier.swipeable(
//            state = sheetStateToggl,
//            anchors = anchors,
//            orientation = Orientation.Vertical,
//            enabled = sheetStateToggl.currentValue != Hidden,
//            resistance = null
//        )
//    } else {
//        Modifier
//    }
//
//    return this.then(modifier)
//}
//
//@Composable
//private fun Scrim(
//    color: Color,
//    onDismiss: () -> Unit,
//    visible: Boolean
//) {
//    if (color.isSpecified) {
//        val alpha by animateFloatAsState(
//            targetValue = if (visible) 1f else 0f,
//            animationSpec = TweenSpec()
//        )
//        val dismissModifier = if (visible) {
//            Modifier
//                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
//                .semantics(mergeDescendants = true) {
//                    contentDescription = "closeSheet"
//                    onClick { onDismiss(); true }
//                }
//        } else {
//            Modifier
//        }
//
//        Canvas(
//            Modifier
//                .fillMaxSize()
//                .then(dismissModifier)
//        ) {
//            drawRect(color = color, alpha = alpha)
//        }
//    }
//}
//
///**
// * Contains useful Defaults for [TogglModalBottomSheetLayout].
// */
//object ModalBottomSheetDefaults {
//
//    /**
//     * The default elevation used by [TogglModalBottomSheetLayout].
//     */
//    val Elevation = 16.dp
//
//    /**
//     * The default scrim color used by [TogglModalBottomSheetLayout].
//     */
//    val scrimColor: Color
//        @Composable
//        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
//}
//
//@ExperimentalMaterialApi
//val <T> SwipeableState<T>.PreUpPostDownNestedScrollConnection: NestedScrollConnection
//    get() = object : NestedScrollConnection {
//        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
//            val delta = available.toFloat()
//            return if (delta < 0 && source == NestedScrollSource.Drag) {
//                performDrag(delta).toOffset()
//            } else {
//                Offset.Zero
//            }
//        }
//
//        override fun onPostScroll(
//            consumed: Offset,
//            available: Offset,
//            source: NestedScrollSource
//        ): Offset {
//            return if (source == NestedScrollSource.Drag) {
//                performDrag(available.toFloat()).toOffset()
//            } else {
//                Offset.Zero
//            }
//        }
//
//        override suspend fun onPreFling(available: Velocity): Velocity {
//            val toFling = Offset(available.x, available.y).toFloat()
//            return if (toFling < 0 && offset.value > 0) {
//                performFling(velocity = toFling)
//                // since we go to the anchor with tween settling, consume all for the best UX
//                available
//            } else {
//                Velocity.Zero
//            }
//        }
//
//        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
//            performFling(velocity = Offset(available.x, available.y).toFloat())
//            return available
//        }
//
//        private fun Float.toOffset(): Offset = Offset(0f, this)
//
//        private fun Offset.toFloat(): Float = this.y
//    }
//
//@ExperimentalMaterialNavigationApi
//@ExperimentalMaterialApi
//@Composable
//internal fun TogglSheetContentHost(
//    columnHost: ColumnScope,
//    backStackEntry: NavBackStackEntry?,
//    sheetState: TogglModalBottomSheetState,
//    saveableStateHolder: SaveableStateHolder,
//    onSheetShown: (entry: NavBackStackEntry) -> Unit,
//    onSheetDismissed: (entry: NavBackStackEntry) -> Unit,
//) {
//    val scope = rememberCoroutineScope()
//    if (backStackEntry != null) {
//        val currentOnSheetShown by rememberUpdatedState(onSheetShown)
//        val currentOnSheetDismissed by rememberUpdatedState(onSheetDismissed)
//        var hideCalled by remember(backStackEntry) { mutableStateOf(false) }
//        LaunchedEffect(backStackEntry, hideCalled) {
//            val sheetVisibility = snapshotFlow { sheetState.isVisible }
//            sheetVisibility
//                // We are only interested in changes in the sheet's visibility
//                .distinctUntilChanged()
//                // distinctUntilChanged emits the initial value which we don't need
//                .drop(1)
//                // We want to know when the sheet was visible but is not anymore
//                .filter { isVisible -> !isVisible }
//                // Finally, pop the back stack when the sheet has been hidden
//                .collect { if (!hideCalled) currentOnSheetDismissed(backStackEntry) }
//        }
//
//        // We use this signal to know when its (almost) safe to `show` the bottom sheet
//        // It will be set after the sheet's content has been `onGloballyPositioned`
//        val contentPositionedSignal = remember(backStackEntry) {
//            CompletableDeferred<Unit?>()
//        }
//
//        // Whenever the composable associated with the backStackEntry enters the composition, we
//        // want to show the sheet, and hide it when this composable leaves the composition
//        DisposableEffect(backStackEntry) {
//            scope.launch {
//                contentPositionedSignal.await()
//                try {
//                    // If we don't wait for a few frames before calling `show`, we will be too early
//                    // and the sheet content won't have been laid out yet (even with our content
//                    // positioned signal). If a sheet is tall enough to have a HALF_EXPANDED state,
//                    // we might be here before the SwipeableState's anchors have been properly
//                    // calculated, resulting in the sheet animating to the EXPANDED state when
//                    // calling `show`. As a workaround, we wait for a magic number of frames.
//                    // https://issuetracker.google.com/issues/200980998
//                    repeat(AWAIT_FRAMES_BEFORE_SHOW) { awaitFrame() }
//                    sheetState.show()
//                } catch (sheetShowCancelled: CancellationException) {
//                    // There is a race condition in ModalBottomSheetLayout that happens when the
//                    // sheet content changes due to the anchors being re-calculated. This causes an
//                    // animation to run for a short time, cancelling any currently running animation
//                    // such as the one triggered by our `show` call.
//                    // The sheet will still snap to the EXPANDED or HALF_EXPANDED state.
//                    // In that case we want to wait until the sheet is visible. For safety, we only
//                    // wait for 800 milliseconds - if the sheet is not visible until then, something
//                    // has gone horribly wrong.
//                    // https://issuetracker.google.com/issues/200980998
//                    withTimeout(800) {
//                        while (!sheetState.isVisible) {
//                            awaitFrame()
//                        }
//                    }
//                } finally {
//                    // If, for some reason, the sheet is in a state where the animation is still
//                    // running, there is a chance that it is already targeting the EXPANDED or
//                    // HALF_EXPANDED state and will snap to that. In that case we can be fairly
//                    // certain that the sheet will actually end up in that state.
//                    if (sheetState.isVisible || sheetState.willBeVisible) {
//                        currentOnSheetShown(backStackEntry)
//                    }
//                }
//            }
//            onDispose {
//                scope.launch {
//                    hideCalled = true
//                    try {
//                        sheetState.internalHide()
//                    } finally {
//                        hideCalled = false
//                    }
//                }
//            }
//        }
//
//        val content = (backStackEntry.destination as TogglBottomSheetNavigator.Destination).content
//        backStackEntry.LocalOwnersProvider(saveableStateHolder) {
//            Box(Modifier.onGloballyPositioned { contentPositionedSignal.complete(Unit) }) {
//                columnHost.content(backStackEntry)
//            }
//        }
//    } else {
//        EmptySheet()
//    }
//}
//
//@Composable
//private fun EmptySheet() {
//    // The swipeable modifier has a bug where it doesn't support having something with
//    // height = 0
//    // b/178529942
//    // If there are no destinations on the back stack, we need to add something to work
//    // around this
//    Box(Modifier.height(1.dp))
//}
//
//private suspend fun awaitFrame() = withFrameNanos(onFrame = {})
//
///**
// * This magic number has been chosen through painful experiments.
// * - Waiting for 1 frame still results in the sheet fully expanding, which we don't want
// * - Waiting for 2 frames results in the `show` call getting cancelled
// * - Waiting for 3+ frames results in the sheet expanding to the correct state. Success!
// * We wait for a few frames more just to be sure.
// */
//private const val AWAIT_FRAMES_BEFORE_SHOW = 4
//
//// We have the same issue when we are hiding the sheet, but snapTo works better
//@OptIn(ExperimentalMaterialApi::class)
//private suspend fun TogglModalBottomSheetState.internalHide() {
//    snapTo(Hidden)
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//private val TogglModalBottomSheetState.willBeVisible: Boolean
//    get() = targetValue == HalfExpanded || targetValue == Expanded
//
//@ExperimentalMaterialNavigationApi
//fun NavGraphBuilder.togglBottomSheet(
//    route: String,
//    arguments: List<NamedNavArgument> = emptyList(),
//    deepLinks: List<NavDeepLink> = emptyList(),
//    content: @Composable ColumnScope.(backstackEntry: NavBackStackEntry) -> Unit
//) {
//    addDestination(
//        TogglBottomSheetNavigator.Destination(
//            provider[TogglBottomSheetNavigator::class],
//            content
//        ).apply {
//            this.route = route
//            arguments.forEach { (argumentName, argument) ->
//                addArgument(argumentName, argument)
//            }
//            deepLinks.forEach { deepLink ->
//                addDeepLink(deepLink)
//            }
//        }
//    )
//}
