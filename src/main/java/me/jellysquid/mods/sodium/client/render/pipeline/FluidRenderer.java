package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.WorldUtil;
import net.minecraft.block.AbstractFluidBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.embeddedt.embeddium.render.fluid.EmbeddiumFluidSpriteCache;
import org.joml.Vector3d;

import java.util.Arrays;

public class FluidRenderer {

    private static final float EPSILON = 0.001f;

    private static final IBlockColor FLUID_COLOR_PROVIDER = (state, world, pos, tintIndex) -> 0xffffffff;

    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipelineProvider lighters;
    private final BiomeColorBlender biomeColorBlender;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];

    private final EmbeddiumFluidSpriteCache fluidSpriteCache = new EmbeddiumFluidSpriteCache();

    private final BlockColors vanillaBlockColors;

    public FluidRenderer(MinecraftClient client, LightPipelineProvider lighters, BiomeColorBlender biomeColorBlender) {
        int normal = Norm3b.pack(0.0f, 1.0f, 0.0f);

        for (int i = 0; i < 4; i++) {
            this.quad.setNormal(i, normal);
        }

        this.lighters = lighters;
        this.biomeColorBlender = biomeColorBlender;
        this.vanillaBlockColors = BlockColors.INSTANCE;
    }

    private boolean isFluidOccluded(BlockView world, int x, int y, int z, Direction dir, AbstractFluidBlock fluid) {
        BlockPos pos = scratchPos.setPosition(x, y, z);
        BlockState blockState = world.getBlockState(pos);
        BlockPos adjPos = scratchPos.setPosition(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
        AbstractFluidBlock adjFluid = WorldUtil.getFluid(world.getBlockState(adjPos));
        boolean temp = fluid == adjFluid;

        if (blockState.getBlock().getMaterial().isOpaque()) {
            return temp || blockState.getBlock().isSideInvisible(world, pos, dir);
            // fluidlogged or next to water, occlude sides that are solid or the same liquid
        }
        return temp;
    }

    private boolean isSideExposed(BlockView world, int x, int y, int z, Direction dir) {
        BlockPos pos = scratchPos.setPosition(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if (block.getMaterial().isOpaque()) {
            final boolean renderAsFullCube = block.renderAsNormalBlock();

            // Hoist these checks to avoid allocating the shape below
            if (renderAsFullCube) {
                // The top face always be inset, so if the shape above is a full cube it can't possibly occlude
                return dir == Direction.UP;
            } else {
                return true;
            }
        }

        return true;
    }

    public boolean render(BlockView world, BlockState fluidState, BlockPos pos, ChunkModelBuffers buffers) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        AbstractFluidBlock fluid = (AbstractFluidBlock) fluidState.getBlock();

        boolean sfUp = isFluidOccluded(world, posX, posY, posZ, Direction.UP, fluid);
        boolean sfDown = isFluidOccluded(world, posX, posY, posZ, Direction.DOWN, fluid) ||
                !isSideExposed(world, posX, posY, posZ, Direction.DOWN);
        boolean sfNorth = isFluidOccluded(world, posX, posY, posZ, Direction.NORTH, fluid);
        boolean sfSouth = isFluidOccluded(world, posX, posY, posZ, Direction.SOUTH, fluid);
        boolean sfWest = isFluidOccluded(world, posX, posY, posZ, Direction.WEST, fluid);
        boolean sfEast = isFluidOccluded(world, posX, posY, posZ, Direction.EAST, fluid);

        if (sfUp && sfDown && sfEast && sfWest && sfNorth && sfSouth) {
            return false;
        }

        Sprite[] sprites = fluidSpriteCache.getSprites(fluid);
        boolean hc = fluid.getColor() != 0xffffffff;

        boolean rendered = false;

        float h1 = getCornerHeight(world, posX, posY, posZ, fluid);
        float h2 = getCornerHeight(world, posX, posY, posZ + 1, fluid);
        float h3 = getCornerHeight(world, posX + 1, posY, posZ + 1, fluid);
        float h4 = getCornerHeight(world, posX + 1, posY, posZ, fluid);

        float yOffset = sfDown ? 0.0F : EPSILON;

        final ModelQuadViewMutable quad = this.quad;

        LightMode mode = hc && MinecraftClient.isAmbientOcclusionEnabled() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(mode);

        quad.setFlags(0);

        if (!sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP)) {
            h1 -= 0.001F;
            h2 -= 0.001F;
            h3 -= 0.001F;
            h4 -= 0.001F;

            Vec3d velocity = AbstractFluidBlock.getFlowingFluidByMaterial(fluid.getMaterial()).getFluidVec(world, pos);

            Sprite sprite;
            ModelQuadFacing facing;
            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                sprite = sprites[0];
                facing = ModelQuadFacing.UP;
                u1 = sprite.getFrameU(0.0D);
                v1 = sprite.getFrameV(0.0D);
                u2 = u1;
                v2 = sprite.getFrameV(16.0D);
                u3 = sprite.getFrameU(16.0D);
                v3 = v2;
                u4 = u3;
                v4 = v1;
            } else {
                sprite = sprites[1];
                facing = ModelQuadFacing.UNASSIGNED;
                float dir = (float) MathHelper.atan2(velocity.z, velocity.x) - (1.5707964f);
                float sin = MathHelper.sin(dir) * 0.25F;
                float cos = MathHelper.cos(dir) * 0.25F;
                u1 = sprite.getFrameU(8.0F + (-cos - sin) * 16.0F);
                v1 = sprite.getFrameV(8.0F + (-cos + sin) * 16.0F);
                u2 = sprite.getFrameU(8.0F + (-cos + sin) * 16.0F);
                v2 = sprite.getFrameV(8.0F + (cos + sin) * 16.0F);
                u3 = sprite.getFrameU(8.0F + (cos + sin) * 16.0F);
                v3 = sprite.getFrameV(8.0F + (cos - sin) * 16.0F);
                u4 = sprite.getFrameU(8.0F + (cos - sin) * 16.0F);
                v4 = sprite.getFrameV(8.0F + (-cos - sin) * 16.0F);
            }

            float uAvg = (u1 + u2 + u3 + u4) / 4.0F;
            float vAvg = (v1 + v2 + v3 + v4) / 4.0F;
            float s1 = (float) sprites[0].getWidth() / (sprites[0].getMaxU() - sprites[0].getMinU());
            float s2 = (float) sprites[0].getHeight() / (sprites[0].getMaxV() - sprites[0].getMinV());
            float s3 = 4.0F / Math.max(s2, s1);

            u1 = (float) MathUtil.lerp(s3, u1, uAvg);
            u2 = (float) MathUtil.lerp(s3, u2, uAvg);
            u3 = (float) MathUtil.lerp(s3, u3, uAvg);
            u4 = (float) MathUtil.lerp(s3, u4, uAvg);
            v1 = (float) MathUtil.lerp(s3, v1, vAvg);
            v2 = (float) MathUtil.lerp(s3, v2, vAvg);
            v3 = (float) MathUtil.lerp(s3, v3, vAvg);
            v4 = (float) MathUtil.lerp(s3, v4, vAvg);

            quad.setSprite(sprite);

            setVertex(quad, 0, 0.0f, h1, 0.0f, u1, v1);
            setVertex(quad, 1, 0.0f, h2, 1.0F, u2, v2);
            setVertex(quad, 2, 1.0F, h3, 1.0F, u3, v3);
            setVertex(quad, 3, 1.0F, h4, 0.0f, u4, v4);

            //float brightness = 1.0f;

            calculateQuadColors(quad, world, pos, lighter, Direction.UP, 1.0f, hc);
            flushQuad(buffers, quad, facing, false);

            if (WorldUtil.method_15756(world, this.scratchPos.setPosition(posX, posY + 1, posZ), fluid)) {
                setVertex(quad, 3, 0.0f, h1, 0.0f, u1, v1);
                setVertex(quad, 2, 0.0f, h2, 1.0F, u2, v2);
                setVertex(quad, 1, 1.0F, h3, 1.0F, u3, v3);
                setVertex(quad, 0, 1.0F, h4, 0.0f, u4, v4);

                flushQuad(buffers, quad, ModelQuadFacing.DOWN, true);
            }


            rendered = true;
        }

        if (!sfDown) {
            Sprite sprite = sprites[0];

            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();
            quad.setSprite(sprite);

            setVertex(quad, 0, 0.0f, yOffset, 1.0F, minU, maxV);
            setVertex(quad, 1, 0.0f, yOffset, 0.0f, minU, minV);
            setVertex(quad, 2, 1.0F, yOffset, 0.0f, maxU, minV);
            setVertex(quad, 3, 1.0F, yOffset, 1.0F, maxU, maxV);

            //float brightness = 1.0f;

            calculateQuadColors(quad, world, pos, lighter, Direction.DOWN, 1.0f, hc);
            flushQuad(buffers, quad, ModelQuadFacing.DOWN, false);

            rendered = true;
        }

        quad.setFlags(ModelQuadFlags.IS_ALIGNED);

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;

            switch (dir) {
                case NORTH:
                    if (sfNorth) {
                        continue;
                    }

                    c1 = h1;
                    c2 = h4;
                    x1 = 0.0f;
                    x2 = 1.0F;
                    z1 = 0.001f;
                    z2 = z1;
                    break;
                case SOUTH:
                    if (sfSouth) {
                        continue;
                    }

                    c1 = h3;
                    c2 = h2;
                    x1 = 1.0F;
                    x2 = 0.0f;
                    z1 = 0.999f;
                    z2 = z1;
                    break;
                case WEST:
                    if (sfWest) {
                        continue;
                    }

                    c1 = h2;
                    c2 = h1;
                    x1 = 0.001f;
                    x2 = x1;
                    z1 = 1.0F;
                    z2 = 0.0f;
                    break;
                case EAST:
                    if (sfEast) {
                        continue;
                    }

                    c1 = h4;
                    c2 = h3;
                    x1 = 0.999f;
                    x2 = x1;
                    z1 = 0.0f;
                    z2 = 1.0F;
                    break;
                default:
                    continue;
            }

            if (this.isSideExposed(world, posX, posY, posZ, dir)) {
                Sprite sprite = sprites[1];

                float u1 = sprite.getFrameU(0.0D);
                float u2 = sprite.getFrameU(8.0D);
                float v1 = sprite.getFrameV((1.0F - c1) * 16.0F * 0.5F);
                float v2 = sprite.getFrameV((1.0F - c2) * 16.0F * 0.5F);
                float v3 = sprite.getFrameV(8.0D);

                quad.setSprite(sprite);

                this.setVertex(quad, 0, x2, c2, z2, u2, v2);
                this.setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                this.setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                this.setVertex(quad, 3, x1, c1, z1, u1, v1);

                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

                ModelQuadFacing facing = ModelQuadFacing.fromDirection(dir);

                this.calculateQuadColors(quad, world, pos, lighter, dir, br, hc);
                this.flushQuad(buffers, quad, facing, false);

                this.setVertex(quad, 0, x1, c1, z1, u1, v1);
                this.setVertex(quad, 1, x1, yOffset, z1, u1, v3);
                this.setVertex(quad, 2, x2, yOffset, z2, u2, v3);
                this.setVertex(quad, 3, x2, c2, z2, u2, v2);

                this.flushQuad(buffers, quad, facing.getOpposite(), true);

                rendered = true;
            }
        }

        return rendered;
    }

    private void calculateQuadColors(ModelQuadView quad, BlockView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, boolean colorized) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, null, dir, false);

        int[] biomeColors = null;

        if (colorized) {
            BlockState state = world.getBlockState(pos);
            IBlockColor colorProvider = ((BlockColorsExtended) this.vanillaBlockColors).getColorProvider(state);
            boolean containsColoredQuad = false;
            if (colorProvider != null) {
                biomeColors = this.biomeColorBlender.getColors(colorProvider, world, state, pos, quad);
                for (int color : biomeColors) {
                    if (color != 0xFFFFFF) {
                        containsColoredQuad = true;
                        break;
                    }
                }
            }
            if (!containsColoredQuad) {
                biomeColors = this.biomeColorBlender.getColors(FLUID_COLOR_PROVIDER, world, state, pos, quad);
            }
        }

        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = ColorABGR.mul(biomeColors != null ? biomeColors[i] : 0xFFFFFFFF, light.br[i] * brightness);
        }
    }

    private void flushQuad(ChunkModelBuffers buffers, ModelQuadView quad, ModelQuadFacing facing, boolean flip) {
        int vertexIdx, lightOrder;

        if (flip) {
            vertexIdx = 3;
            lightOrder = -1;
        } else {
            vertexIdx = 0;
            lightOrder = 1;
        }

        ModelVertexSink sink = buffers.getSink(facing);
        sink.ensureCapacity(4);

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            int color = this.quadColors[vertexIdx];

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = this.quadLightData.lm[vertexIdx];

            sink.writeQuad(x, y, z, color, u, v, light);

            vertexIdx += lightOrder;
        }

        sink.flush();
    }

    private void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }

    private float getCornerHeight(BlockView world, int x, int y, int z, AbstractFluidBlock fluid) {
        int samples = 0;
        float totalHeight = 0.0F;

        for (int i = 0; i < 4; ++i) {
            int x2 = x - (i & 1);
            int z2 = z - (i >> 1 & 1);

            Block block = world.getBlockState(this.scratchPos.setPosition(x2, y + 1, z2)).getBlock();
            if (block.getMaterial() == fluid.getMaterial()) {
                return 1.0F;
            }

            BlockPos pos = this.scratchPos.setPosition(x2, y, z2);

            BlockState blockState = world.getBlockState(pos);
            Material material = blockState.getBlock().getMaterial();

            if (fluid.getMaterial() != material) {
                if (!material.isSolid()) {
                    ++samples;
                    ++totalHeight;
                }
            } else {
                int height = blockState.get(AbstractFluidBlock.LEVEL);

                if (height >= 8 || height == 0) {
                    totalHeight += AbstractFluidBlock.getHeightPercent(height) * 10.0F;
                    samples += 10;
                } else {
                    totalHeight += AbstractFluidBlock.getHeightPercent(height);
                    ++samples;
                }
            }
        }

        return 1f - totalHeight / (float) samples;
    }
}
