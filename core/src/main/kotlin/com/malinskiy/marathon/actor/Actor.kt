package com.malinskiy.marathon.actor

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.selects.SelectClause2
import kotlin.coroutines.experimental.CoroutineContext

abstract class Actor<in T>(parent: Job? = null,
                           context: CoroutineContext) : SendChannel<T> {
    protected abstract suspend fun receive(msg: T)
    private val actorJob = Job(parent)

    private val delegate = actor<T>(
            capacity = Channel.UNLIMITED,
            parent = actorJob,
            context = context
    ) {
        for (msg in channel) {
            receive(msg)
        }
    }

    override val isClosedForSend: Boolean
        get() = delegate.isClosedForSend
    override val isFull: Boolean
        get() = delegate.isFull
    override val onSend: SelectClause2<T, SendChannel<T>>
        get() = delegate.onSend

    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        delegate.invokeOnClose(handler)
    }

    override fun close(cause: Throwable?): Boolean = actorJob.cancel()

    override fun offer(element: T): Boolean = delegate.offer(element)

    override suspend fun send(element: T) = delegate.send(element)
}
