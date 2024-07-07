package net.minecraft.client.renderer.color;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.Leaves1Block;
import net.minecraft.block.PlanksBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FlowerPotBlockEntity;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.item.Item;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Taken from Minecraft 1.12.2
 */
public class BlockColors implements BlockColorsExtended {
    private final IdList<IBlockColor> mapBlockColors = new IdList<>();
    private final Reference2ReferenceMap<Block, IBlockColor> blocksToColor;
    private static final IBlockColor DEFAULT_PROVIDER = (state, view, pos, tint) -> -1;

    public static BlockColors INSTANCE;

    static {
        init();
    }

    public BlockColors() {
        this.blocksToColor = new Reference2ReferenceOpenHashMap<>();
        this.blocksToColor.defaultReturnValue(DEFAULT_PROVIDER);
    }

    public static BlockColors init() {
        INSTANCE = new BlockColors();
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            DoublePlantBlock.DoublePlantType type = state.get(DoublePlantBlock.VARIANT);
            return worldIn != null && pos != null && (type == DoublePlantBlock.DoublePlantType.GRASS || type == DoublePlantBlock.DoublePlantType.FERN) ? BiomeColors.getGrassColor(worldIn, state.get(DoublePlantBlock.HALF) == DoublePlantBlock.HalfType.UPPER ? pos.down() : pos) : -1;
        }, Blocks.DOUBLE_PLANT);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            if (worldIn != null && pos != null) {
                BlockEntity tileentity = worldIn.getBlockEntity(pos);

                if (tileentity instanceof FlowerPotBlockEntity) {
                    Item item = ((FlowerPotBlockEntity) tileentity).getItem();
                    BlockState iblockstate = Block.getBlockFromItem(item).getDefaultState();
                    return INSTANCE.colorMultiplier(iblockstate, worldIn, pos, tintIndex);
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }, Blocks.FLOWER_POT);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getGrassColor(worldIn, pos) : GrassColors.getColor(0.5D, 1.0D), Blocks.GRASS);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            PlanksBlock.WoodType blockplanks$enumtype = state.get(Leaves1Block.VARIANT);
            if (blockplanks$enumtype == PlanksBlock.WoodType.SPRUCE) {
                return FoliageColors.getSpruceColor();
            } else if (blockplanks$enumtype == PlanksBlock.WoodType.BIRCH) {
                return FoliageColors.getBirchColor();
            } else {
                return worldIn != null && pos != null ? BiomeColors.getFoliageColor(worldIn, pos) : FoliageColors.getDefaultColor();
            }
        }, Blocks.LEAVES);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) ->
                worldIn != null && pos != null ? BiomeColors.getFoliageColor(worldIn, pos) : FoliageColors.getDefaultColor(), Blocks.LEAVES2);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) ->
                worldIn != null && pos != null ? BiomeColors.getWaterColor(worldIn, pos) : -1, Blocks.WATER, Blocks.FLOWING_WATER);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) ->
                worldIn != null && pos != null ? BiomeColors.getGrassColor(worldIn, pos) : -1, Blocks.SUGARCANE);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            int i = state.get(StemBlock.AGE);
            int j = i * 32;
            int k = 255 - i * 8;
            int l = i * 4;
            return j << 16 | k << 8 | l;
        }, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            if (worldIn != null && pos != null) {
                return BiomeColors.getGrassColor(worldIn, pos);
            }
            return state.get(TallPlantBlock.TYPE) == TallPlantBlock.GrassType.DEAD_BUSH ? 0xffffff :
                    GrassColors.getColor(0.5D, 1.0D);
        }, Blocks.TALLGRASS);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getFoliageColor(worldIn, pos) : FoliageColors.getDefaultColor(), Blocks.VINE);
        INSTANCE.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? 0x208030 : 0x71c35c, Blocks.LILY_PAD);
        return INSTANCE;
    }

    public int getColor(BlockState p_189991_1_, World p_189991_2_, BlockPos p_189991_3_) {
        IBlockColor iblockcolor = this.mapBlockColors.fromId(Block.getIdByBlock(p_189991_1_.getBlock()));

        if (iblockcolor != null) {
            return iblockcolor.colorMultiplier(p_189991_1_, null, null, 0);
        } else {
            MaterialColor mapcolor = p_189991_1_.getBlock().getMaterialColor(p_189991_1_);
            return mapcolor != null ? mapcolor.color : -1;
        }
    }

    public int colorMultiplier(BlockState state, @Nullable BlockView blockAccess, @Nullable BlockPos pos, int renderPass) {
        IBlockColor iblockcolor = this.mapBlockColors.fromId(Block.getIdByBlock(state.getBlock()));
        return iblockcolor == null ? -1 : iblockcolor.colorMultiplier(state, blockAccess, pos, renderPass);
    }

    public void registerBlockColorHandler(IBlockColor blockColor, Block... blocksIn) {
        synchronized (this.blocksToColor) {
            for (Block block : blocksIn) {
                if (blockColor != null)
                    this.blocksToColor.put(block, blockColor);
            }
        }
        for (Block block : blocksIn) {
            this.mapBlockColors.set(blockColor, Block.getIdByBlock(block));
        }
    }

    @Override
    public IBlockColor getColorProvider(BlockState state) {
        return blocksToColor.get(state.getBlock());
    }
}