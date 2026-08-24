package dev.client.api.nullcry.rotation;

import dev.client.api.nullcry.ClientApi;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RaytracingUtil implements ClientApi {
    
    public static HitResult raycast(double maxDistance, Angle rotation, boolean includeFluids) {
        if (mc.player == null || mc.world == null) {
            return BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }
        
        Vec3d start = mc.player.getEyePos();
        Vec3d direction = rotation.toVector();
        Vec3d end = start.add(direction.multiply(maxDistance));
        
        RaycastContext.FluidHandling fluidHandling = includeFluids 
            ? RaycastContext.FluidHandling.ANY 
            : RaycastContext.FluidHandling.NONE;
            
        RaycastContext context = new RaycastContext(
            start, 
            end, 
            RaycastContext.ShapeType.OUTLINE, 
            fluidHandling, 
            mc.player
        );
        
        return mc.world.raycast(context);
    }
}
