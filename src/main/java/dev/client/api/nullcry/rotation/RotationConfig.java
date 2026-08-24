package dev.client.api.nullcry.rotation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RotationConfig {
    private boolean moveCorrection;
    private boolean freeCorrection;
    
    public RotationConfig(boolean correction) {
        this.moveCorrection = correction;
        this.freeCorrection = correction;
    }
}
