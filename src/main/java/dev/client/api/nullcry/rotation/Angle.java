package dev.client.api.nullcry.rotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.math.Vec3d;

@Data
@AllArgsConstructor
public class Angle {
    private float yaw;
    private float pitch;

    public Vec3d toVector() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        
        return new Vec3d(x, y, z);
    }
}
