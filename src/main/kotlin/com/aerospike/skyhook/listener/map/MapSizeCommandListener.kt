package com.aerospike.skyhook.listener.map

import com.aerospike.client.Key
import com.aerospike.client.Record
import com.aerospike.client.cdt.MapOperation
import com.aerospike.client.listener.RecordListener
import com.aerospike.skyhook.command.RequestCommand
import com.aerospike.skyhook.config.AerospikeContext
import com.aerospike.skyhook.listener.BaseListener
import io.netty.channel.ChannelHandlerContext

class MapSizeCommandListener(
    aeroCtx: AerospikeContext,
    ctx: ChannelHandlerContext
) : BaseListener(aeroCtx, ctx), RecordListener {

    override fun handle(cmd: RequestCommand) {
        require(cmd.argCount == 2) { argValidationErrorMsg(cmd) }

        val key = createKey(cmd.key)
        val operation = MapOperation.size(aeroCtx.bin)

        aeroCtx.client.operate(
            null, this, null,
            key, operation
        )
    }

    override fun onSuccess(key: Key?, record: Record?) {
        if (record == null) {
            writeLong(ctx, 0L)
            ctx.flush()
        } else {
            try {
                writeResponse(record.bins[aeroCtx.bin])
                ctx.flush()
            } catch (e: Exception) {
                closeCtx(e)
            }
        }
    }
}
