package me.jellysquid.mods.sodium.client.render.pipeline;

import java.util.List;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BaseLeavesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Vector3i;
import org.lwjgl.util.vector.Vector3f;

public class BlockRenderer {
    private final BlockColorsExtended blockColors;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final BiomeColorBlender biomeColorBlender;
    private final LightPipelineProvider lighters;

    private final BlockOcclusionCache occlusionCache;

    private final boolean useAmbientOcclusion;

    public BlockRenderer(MinecraftClient client, LightPipelineProvider lighters, BiomeColorBlender biomeColorBlender) {
        this.blockColors = BlockColors.INSTANCE;
        this.biomeColorBlender = biomeColorBlender;

        this.lighters = lighters;

        this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();

        this.occlusionCache = new BlockOcclusionCache();
    }

    public boolean renderModel(BlockView world, BlockState state, BlockPos pos, BakedModel model, ChunkModelBuffers buffers, boolean cull) {
        LightMode mode = getLightingMode(state, model, world, pos);
        LightPipeline lighter = lighters.getLighter(mode);

        Vector3f offset = new Vector3f();
        Block.OffsetType offsetType = state.getBlock().getOffsetType();

        if (offsetType != Block.OffsetType.NONE) {
            int x = pos.getX();
            int z = pos.getZ();
            // Taken from MathHelper.hashCode()
            long i = (x * 3129871L) ^ z * 116129781L;
            i = i * i * 42317861L + i * 11L;

            offset.x += (((i >> 16 & 15L) / 15.0F) - 0.5f) * 0.5f;
            offset.z += (((i >> 24 & 15L) / 15.0F) - 0.5f) * 0.5f;

            if (offsetType == Block.OffsetType.XYZ) {
                offset.y += (((i >> 20 & 15L) / 15.0F) - 1.0f) * 0.2f;
            }
        }

        boolean rendered = false;

        // Use Sodium's default render path
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> sided = model.getByDirection(dir);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || occlusionCache.shouldDrawSide(world, pos, dir)) {
                renderQuadList(world, state, pos, lighter, offset, buffers, sided, dir);

                rendered = true;
            }
        }

        List<BakedQuad> all = model.getQuads();

        if (!all.isEmpty()) {
            renderQuadList(world, state, pos, lighter, offset, buffers, all, null);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(BlockView world, BlockState state, BlockPos pos, LightPipeline lighter, Vector3f offset,
                                ChunkModelBuffers buffers, List<BakedQuad> quads, Direction cullFace) {
    	ModelQuadFacing facing = cullFace == null ? ModelQuadFacing.UNASSIGNED : ModelQuadFacing.fromDirection(cullFace);
        IBlockColor colorizer = null;

        ModelVertexSink sink = buffers.getSink(facing);
        sink.ensureCapacity(quads.size() * 4);

        ChunkRenderData.Builder renderData = buffers.getRenderData();

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);

            Direction quadFace = quad.getFace();

            QuadLightData light = this.cachedQuadLightData;

            // todo this is probably where brightness should, quad.hasBrightness()
            // flat lighter will use max brightness if shade is false
            lighter.calculate((ModelQuadView) quad, pos, light, cullFace, quadFace, quad.hasColor());

            if (quad.hasColor() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, sink, offset, colorizer, quad, light, renderData);
        }

        sink.flush();
    }

    private void renderQuad(BlockView world, BlockState state, BlockPos pos, ModelVertexSink sink, Vector3f offset,
                            IBlockColor colorProvider, BakedQuad bakedQuad, QuadLightData light, ChunkRenderData.Builder renderData) {
        ModelQuadView src = (ModelQuadView) bakedQuad;

        ModelQuadOrientation order = ModelQuadOrientation.orient(light.br);

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = biomeColorBlender.getColors(colorProvider, world, state, pos, src);
        }

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = order.getVertexIndex(dstIndex);

            float x = src.getX(srcIndex) + offset.x;
            float y = src.getY(srcIndex) + offset.y;
            float z = src.getZ(srcIndex) + offset.z;

            // todo refactor into coloring respective class
            int color;
            if (state.getBlock().isTranslucent()) {
                color = ColorABGR.mul(colors != null ? colors[srcIndex] : src.getColor(srcIndex), 1.0f);
            } else {
                color = ColorABGR.mul(colors != null ? colors[srcIndex] : src.getColor(srcIndex), light.br[srcIndex]);
            }

            float u = src.getTexU(srcIndex);
            float v = src.getTexV(srcIndex);

            int lm = light.lm[srcIndex];

            sink.writeQuad(x, y, z, color, u, v, lm);
        }

        Sprite sprite = src.rubidium$getSprite();

        if (sprite != null) {
            renderData.addSprite(sprite);
        }
    }

    private LightMode getLightingMode(BlockState state, BakedModel model, BlockView world, BlockPos pos) {
        Block block = state.getBlock();
        if (useAmbientOcclusion && model.useAmbientOcclusion() && block.getLightLevel() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
