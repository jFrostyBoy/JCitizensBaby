package jfbdev.jcitizensbaby;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

@TraitName("scale")
public class ScaleTrait extends Trait {

    @Persist("scale")
    private double scale = 1.0;

    public ScaleTrait() {
        super("scale");
    }

    public void setScale(double newScale) {
        this.scale = Math.max(0.01, newScale);
        applyScale();
    }

    void applyScale() {
        if (npc == null || !npc.isSpawned() || !(npc.getEntity() instanceof LivingEntity entity)) return;

        AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_SCALE);
        if (attr != null) {
            attr.setBaseValue(scale);
        }
    }

    @Override
    public void onSpawn() {
        applyScale();
    }

    @Override
    public void onAttach() {
        applyScale();
    }
}