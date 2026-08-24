package dev.client.api.nullcry.rotation;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.modules.Module;
import dev.other.task.TaskPriority;
import net.minecraft.entity.Entity;

public class RotationController implements ClientApi {
    public static final RotationController INSTANCE = new RotationController();
    
    private Angle currentRotation;
    private Angle previousRotation;
    
    public void rotateTo(Angle angle, RotationConfig config, TaskPriority priority, Module module) {
        if (mc.player == null) return;
        
        this.previousRotation = this.currentRotation != null ? this.currentRotation : new Angle(mc.player.getYaw(), mc.player.getPitch());
        this.currentRotation = angle;
        mc.player.setYaw(angle.getYaw());
        mc.player.setPitch(angle.getPitch());
    }
    
    public void rotateTo(Angle angle, Entity target, int smoothness, RotationConfig config, TaskPriority priority, Module module) {
        rotateTo(angle, config, priority, module);
    }
    
    public Angle getRotation() {
        if (mc.player == null) {
            return new Angle(0, 0);
        }
        return currentRotation != null ? currentRotation : new Angle(mc.player.getYaw(), mc.player.getPitch());
    }
    
    public Angle getPreviousRotation() {
        if (mc.player == null) {
            return new Angle(0, 0);
        }
        return previousRotation != null ? previousRotation : getRotation();
    }
    
    public Angle getCurrentAngle() {
        return getRotation();
    }
    
    public Angle getMoveRotation() {
        return getRotation();
    }
    
    public RotationPlan getCurrentRotationPlan() {
        return new RotationPlan();
    }
    
    public void clear() {
        this.currentRotation = null;
        this.previousRotation = null;
    }
}
