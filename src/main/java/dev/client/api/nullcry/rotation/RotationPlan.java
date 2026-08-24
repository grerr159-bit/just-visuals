package dev.client.api.nullcry.rotation;

// Заглушка для совместимости с миксинами
public class RotationPlan {
    private boolean moveCorrection = false;
    private boolean freeCorrection = false;
    
    public boolean isMoveCorrection() {
        return moveCorrection;
    }
    
    public boolean isFreeCorrection() {
        return freeCorrection;
    }
}
