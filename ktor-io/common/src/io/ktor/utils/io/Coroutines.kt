package io.ktor.utils.io

import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * A coroutine job that is reading from a byte channel
 */
public interface ReaderJob : Job {
    /**
     * A reference to the channel that this coroutine is reading from
     */
    public val channel: ByteWriteChannel
}

/**
 * A coroutine job that is writing to a byte channel
 */
public interface WriterJob : Job {
    /**
     * A reference to the channel that this coroutine is writing to
     */
    public val channel: ByteReadChannel
}

public interface ReaderScope : CoroutineScope {
    public val channel: ByteReadChannel
}

public interface WriterScope : CoroutineScope {
    public val channel: ByteWriteChannel
}

public fun CoroutineScope.reader(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    channel: ByteChannel,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = launchChannel(coroutineContext, channel, attachJob = false, block = block)

public fun CoroutineScope.reader(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    autoFlush: Boolean = false,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = launchChannel(coroutineContext, ByteChannel(autoFlush), attachJob = true, block = block)


public fun CoroutineScope.writer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    channel: ByteChannel,
    block: suspend WriterScope.() -> Unit
): WriterJob = launchChannel(coroutineContext, channel, attachJob = false, block = block)

public fun CoroutineScope.writer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    autoFlush: Boolean = false,
    block: suspend WriterScope.() -> Unit
): WriterJob = launchChannel(coroutineContext, ByteChannel(autoFlush), attachJob = true, block = block)

/**
 * @param S not exactly safe (unchecked cast is used) so should be [ReaderScope] or [WriterScope]
 */
private fun <S : CoroutineScope> CoroutineScope.launchChannel(
    context: CoroutineContext,
    channel: ByteChannel,
    attachJob: Boolean,
    block: suspend S.() -> Unit
): ChannelJob {
    val job = launch(context) {
        if (attachJob) {
            channel.attachJob(coroutineContext[Job]!!)
        }
        try {
            @Suppress("UNCHECKED_CAST")
            block(ChannelScope(this, channel) as S)
        } catch (cause: Throwable) {
            channel.close(cause)
            throw cause
        } finally {
            channel.close()
            channel.closedCause?.let { throw it }
        }
    }

    return ChannelJob(job, channel)
}

private class ChannelScope(
    delegate: CoroutineScope,
    override val channel: ByteChannel
) : ReaderScope, WriterScope, CoroutineScope by delegate

private class ChannelJob(
    private val delegate: Job,
    override val channel: ByteChannel
) : ReaderJob, WriterJob, Job by delegate {
    override fun toString(): String = "ChannelJob[$delegate]"
}
