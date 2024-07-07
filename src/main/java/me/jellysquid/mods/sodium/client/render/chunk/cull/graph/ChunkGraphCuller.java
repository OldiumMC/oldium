package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkCuller;
import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ChunkGraphCuller implements ChunkCuller {
    private final Long2ObjectMap<ChunkGraphNode> nodes = new Long2ObjectOpenHashMap<>();

    private final ChunkGraphIterationQueue visible = new ChunkGraphIterationQueue();
    private final World world;
    private final int renderDistance;

    private FrustumExtended frustum;
    private boolean useOcclusionCulling;

    private int activeFrame = 0;
    private int centerChunkX, centerChunkY, centerChunkZ;

    public ChunkGraphCuller(World world, int renderDistance) {
        this.world = world;
        this.renderDistance = renderDistance;
    }

    @Override
    public IntArrayList computeVisible(Vec3d cameraPos, FrustumExtended frustum, int frame, boolean spectator) {
        this.initSearch(cameraPos, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.visible;

        for (int i = 0; i < queue.size(); i++) {
            ChunkGraphNode node = queue.getNode(i);
            short cullData = node.computeQueuePop();

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                if (useOcclusionCulling && (cullData & (1 << dir.ordinal())) == 0) {
                    continue;
                }

                ChunkGraphNode adj = node.getConnectedNode(dir);

                if (adj != null && isWithinRenderDistance(adj)) {
                    bfsEnqueue(node, adj, dir.getOpposite(), cullData);
                }
            }
        }

        return this.visible.getOrderedIdList();
    }

    private boolean isWithinRenderDistance(ChunkGraphNode adj) {
        int x = Math.abs(adj.getChunkX() - centerChunkX);
        int z = Math.abs(adj.getChunkZ() - centerChunkZ);

        return x <= renderDistance && z <= renderDistance;
    }

    private void initSearch(Vec3d cameraPos, FrustumExtended frustum, int frame, boolean spectator) {
        this.activeFrame = frame;
        this.frustum = frustum;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        this.visible.clear();

        BlockPos origin = new BlockPos(cameraPos.x, cameraPos.y, cameraPos.z);

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        this.centerChunkX = chunkX;
        this.centerChunkY = chunkY;
        this.centerChunkZ = chunkZ;

        ChunkGraphNode rootNode = getNode(chunkX, chunkY, chunkZ);

        if (rootNode != null) {
            rootNode.resetCullingState();
            rootNode.setLastVisibleFrame(frame);

            if (spectator && world.getBlockState(origin).getBlock().hasTransparency()) {
                useOcclusionCulling = false;
            }

            visible.add(rootNode);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, 0, 15);

            List<ChunkGraphNode> bestNodes = new ArrayList<>();

            for (int x = -renderDistance; x <= renderDistance; ++x) {
                for (int z = -renderDistance; z <= renderDistance; ++z) {
                    ChunkGraphNode node = getNode(chunkX + x, chunkY, chunkZ + z);

                    if (node == null || node.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    node.resetCullingState();
                    node.setLastVisibleFrame(frame);

                    bestNodes.add(node);
                }
            }

            bestNodes.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (ChunkGraphNode node : bestNodes) {
                visible.add(node);
            }
        }
    }


    private void bfsEnqueue(ChunkGraphNode parent, ChunkGraphNode node, Direction flow, short parentalData) {
        if (node.getLastVisibleFrame() == activeFrame) {
            node.updateCullingState(flow, parentalData);
            return;
        }
        node.setLastVisibleFrame(activeFrame);

        if (node.isCulledByFrustum(frustum)) {
            return;
        }

        node.setCullingState(parentalData);
        node.updateCullingState(flow, parentalData);

        this.visible.add(node);
    }

    private void connectNeighborNodes(ChunkGraphNode node) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkGraphNode adj = this.findAdjacentNode(node, dir);

            if (adj != null) {
                adj.setAdjacentNode(dir.getOpposite(), node);
            }

            node.setAdjacentNode(dir, adj);
        }
    }

    private void disconnectNeighborNodes(ChunkGraphNode node) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkGraphNode adj = node.getConnectedNode(dir);

            if (adj != null) {
                adj.setAdjacentNode(dir.getOpposite(), null);
            }

            node.setAdjacentNode(dir, null);
        }
    }

    private ChunkGraphNode findAdjacentNode(ChunkGraphNode node, Direction dir) {
        return this.getNode(node.getChunkX() + dir.getOffsetX(), node.getChunkY() + dir.getOffsetY(), node.getChunkZ() + dir.getOffsetZ());
    }

    private ChunkGraphNode getNode(int x, int y, int z) {
        return nodes.get(ChunkSectionPos.asLong(x, y, z));
    }

    @Override
    public void onSectionStateChanged(int x, int y, int z, ChunkOcclusionData occlusionData) {
        ChunkGraphNode node = this.getNode(x, y, z);

        if (node != null) {
            node.setOcclusionData(occlusionData);
        }
    }

    @Override
    public void onSectionLoaded(int x, int y, int z, int id) {
        ChunkGraphNode node = new ChunkGraphNode(x, y, z, id);
        ChunkGraphNode prev;

        if ((prev = nodes.put(ChunkSectionPos.asLong(x, y, z), node)) != null) {
            disconnectNeighborNodes(prev);
        }

        connectNeighborNodes(node);
    }

    @Override
    public void onSectionUnloaded(int x, int y, int z) {
        ChunkGraphNode node = nodes.remove(ChunkSectionPos.asLong(x, y, z));

        if (node != null) {
            disconnectNeighborNodes(node);
        }
    }

    @Override
    public boolean isSectionVisible(int x, int y, int z) {
        ChunkGraphNode render = getNode(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == activeFrame;
    }
}
