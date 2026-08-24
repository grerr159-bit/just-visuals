package dev.client.ui.particle;

public class MenuParticle {
    public float x, y;
    public float vx, vy;
    public float size;
    public float alpha;
    
    public MenuParticle() {
    }
    
    public MenuParticle(float x, float y, float vx, float vy, float size, float alpha) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.alpha = alpha;
    }
}