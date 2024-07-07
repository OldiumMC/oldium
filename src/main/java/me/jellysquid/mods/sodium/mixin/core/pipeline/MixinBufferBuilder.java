package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.VertexType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements VertexBufferView, VertexDrain {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("WorldRenderer");

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private VertexFormat format;

    @Shadow
    private int vertexCount;

    @Unique
    private static int roundBufferSize(int amount) {
        int i = 0x200000;
        if (amount == 0) {
            return i;
        } else {
            if (amount < 0) {
                i *= -1;
            }

            int j = amount % i;
            return j == 0 ? amount : amount + i - j;
        }
    }
    
    @Override
    public boolean oldium$ensureBufferCapacity(int bytes) {
    	if(format != null) {
            // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
            bytes += format.getVertexSizeInteger();
        }

        if (this.vertexCount * this.format.getVertexSizeInteger() + bytes <= this.buffer.capacity()) {
            return false;
        }

        int newSize = this.buffer.capacity() + roundBufferSize(bytes);

        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.buffer.capacity(), newSize);

        this.buffer.position(0);

        ByteBuffer byteBuffer = GlAllocationUtils.allocateByteBuffer(newSize);
        byteBuffer.put(this.buffer);
        byteBuffer.rewind();

        this.buffer = byteBuffer;

        return true;
    }

    @Override
    public ByteBuffer oldium$getDirectBuffer() {
        return this.buffer;
    }

    @Override
    public int oldium$getWriterPosition() {
        return this.vertexCount * this.format.getVertexSizeInteger();
    }

    @Override
    public BufferVertexFormat oldium$getVertexFormat() {
        return BufferVertexFormat.from(this.format);
    }

    @Override
    public void oldium$flush(int vertexCount, BufferVertexFormat format) {
        if (BufferVertexFormat.from(this.format) != format) {
            throw new IllegalStateException("Mis-matched vertex format (expected: [" + format + "], currently using: [" + this.format + "])");
        }

        this.vertexCount += vertexCount;
        //this.elementOffset += vertexCount * format.getStride();
    }

    @Override
    public <T extends VertexSink> T oldium$createSink(VertexType<T> factory) {
        BlittableVertexType<T> blittable = factory.asBlittable();

        if (blittable != null && blittable.getBufferVertexFormat() == this.oldium$getVertexFormat())  {
            return blittable.createBufferWriter(this, SodiumClientMod.isDirectMemoryAccessEnabled());
        }

        return factory.createFallbackWriter((BufferBuilder) (Object) this);
    }
}
