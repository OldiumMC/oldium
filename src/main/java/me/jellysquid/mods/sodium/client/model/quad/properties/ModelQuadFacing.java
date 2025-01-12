package me.jellysquid.mods.sodium.client.model.quad.properties;

import net.minecraft.util.math.Direction;

public enum ModelQuadFacing {
    UP,
    DOWN,
    EAST,
    WEST,
    SOUTH,
    NORTH,
    UNASSIGNED;

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();
    public static final int COUNT = VALUES.length;

    public static ModelQuadFacing fromDirection(Direction dir) {
        if(dir == Direction.DOWN){
            return ModelQuadFacing.DOWN;
        }
        else if(dir == Direction.UP){
            return ModelQuadFacing.UP;
        }
        else if(dir == Direction.NORTH){
            return ModelQuadFacing.NORTH;
        }
        else if(dir == Direction.SOUTH){
            return ModelQuadFacing.SOUTH;
        }
        else if(dir == Direction.WEST){
            return ModelQuadFacing.WEST;
        }
        else if(dir == Direction.EAST){
            return ModelQuadFacing.EAST;
        }
        else{
            return ModelQuadFacing.UNASSIGNED;
        }
    }
    
    public ModelQuadFacing getOpposite() {
        if(this == ModelQuadFacing.DOWN){
            return ModelQuadFacing.UP;
        }
        else if(this == ModelQuadFacing.UP){
            return ModelQuadFacing.DOWN;
        }
        else if(this == ModelQuadFacing.NORTH){
            return ModelQuadFacing.SOUTH;
        }
        else if(this == ModelQuadFacing.SOUTH){
            return ModelQuadFacing.NORTH;
        }
        else if(this == ModelQuadFacing.WEST){
            return ModelQuadFacing.EAST;
        }
        else if(this == ModelQuadFacing.EAST){
            return ModelQuadFacing.WEST;
        }
        else{
            return ModelQuadFacing.UNASSIGNED;
        }
    }
}
