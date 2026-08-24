package dev.client.api.nullcry.rotation;

import net.minecraft.util.math.Vec3d;

public class AngleUtil {
    public static Angle fromVec3d(Vec3d vec) {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;
        
        double horizontalDistance = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        float pitch = (float) Math.toDegrees(Math.atan2(-y, horizontalDistance));
        
        return new Angle(yaw, pitch);
    }
    
    public static Angle calculateAngle(Vec3d vec) {
        return fromVec3d(vec);
    }
}
