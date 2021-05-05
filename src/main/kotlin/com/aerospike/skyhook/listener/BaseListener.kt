package com.aerospike.skyhook.listener

import com.aerospike.client.*
import com.aerospike.client.policy.RecordExistsAction
import com.aerospike.client.policy.WritePolicy
import com.aerospike.skyhook.command.RequestCommand
import com.aerospike.skyhook.config.AerospikeContext
import com.aerospike.skyhook.handler.CommandHandler
import com.aerospike.skyhook.handler.NettyResponseWriter
import com.aerospike.skyhook.pipeline.AerospikeChannelInitializer.Companion.aeroCtxAttrKey
import com.aerospike.skyhook.pipeline.AerospikeChannelInitializer.Companion.authDetailsAttrKey
import com.aerospike.skyhook.pipeline.AerospikeChannelInitializer.Companion.clientPoolAttrKey
import io.netty.channel.ChannelHandlerContext
import mu.KotlinLogging
import java.io.IOException

abstract class BaseListener(
    ctx: ChannelHandlerContext
) : NettyResponseWriter(ctx), CommandHandler {

    companion object {

        @JvmStatic
        val log = KotlinLogging.logger {}

        @JvmStatic
        fun argValidationErrorMsg(cmd: RequestCommand): String {
            return "${cmd.command} arguments"
        }

        @JvmStatic
        internal val updateOnlyPolicy = run {
            val updateOnlyPolicy = getWritePolicy()
            updateOnlyPolicy.recordExistsAction = RecordExistsAction.UPDATE_ONLY
            updateOnlyPolicy
        }

        @JvmStatic
        internal val createOnlyPolicy = run {
            val updateOnlyPolicy = getWritePolicy()
            updateOnlyPolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY
            updateOnlyPolicy
        }

        @JvmStatic
        internal val defaultWritePolicy: WritePolicy = getWritePolicy()

        @JvmStatic
        internal fun getWritePolicy(): WritePolicy {
            val writePolicy = WritePolicy()
            writePolicy.sendKey = true
            return writePolicy
        }
    }

    protected fun stringTypeBin(): Bin {
        return Bin(aeroCtx.typeBin, ValueType.STRING.str)
    }

    protected fun stringTypeOp(): Operation {
        return Operation.put(stringTypeBin())
    }

    protected fun listTypeOp(): Operation {
        return Operation.put(Bin(aeroCtx.typeBin, ValueType.LIST.str))
    }

    protected fun setTypeOp(): Operation {
        return Operation.put(Bin(aeroCtx.typeBin, ValueType.SET.str))
    }

    protected fun zsetTypeOp(): Operation {
        return Operation.put(Bin(aeroCtx.typeBin, ValueType.ZSET.str))
    }

    protected fun hashTypeOp(): Operation {
        return Operation.put(Bin(aeroCtx.typeBin, ValueType.HASH.str))
    }

    protected fun streamTypeOp(): Operation {
        return Operation.put(Bin(aeroCtx.typeBin, ValueType.STREAM.str))
    }

    protected val aeroCtx: AerospikeContext by lazy {
        ctx.channel().attr(aeroCtxAttrKey).get()
    }

    protected val client: IAerospikeClient by lazy {
        ctx.channel().attr(clientPoolAttrKey).get().getClient(
            ctx.channel().attr(authDetailsAttrKey).get()
        )
    }

    @Throws(IOException::class)
    protected open fun writeResponse(mapped: Any?) {
        writeObject(mapped)
    }

    @Throws(IOException::class)
    protected open fun writeError(e: AerospikeException?) {
        writeErrorString("Internal error")
    }

    open fun onFailure(exception: AerospikeException?) {
        try {
            log.debug { exception }
            writeError(exception)
            flushCtxTransactionAware()
        } catch (e: IOException) {
            closeCtx(e)
        }
    }

    protected fun createKey(key: Value): Key {
        return Key(aeroCtx.namespace, aeroCtx.set, key)
    }

    protected fun createKey(key: ByteArray): Key {
        return createKey(Value.get(String(key)))
    }
}
