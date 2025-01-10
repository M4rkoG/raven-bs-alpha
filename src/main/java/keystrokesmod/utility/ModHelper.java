package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;

public class ModHelper {
    private final Minecraft mc;

    public ModHelper(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!ModuleManager.bHop.isEnabled() && ModuleManager.bHop.mode.getInput() == 3 && ModuleManager.bHop.didMove) {
            int simpleY = (int) Math.round((e.posY % 1) * 10000);

            if (mc.thePlayer.hurtTime == 0) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        ModuleManager.bHop.lowHopping = false;
                        break;
                }
            }
        }

        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= 0 && Utils.isMoving() && ModuleManager.bHop.isEnabled()) {
                ModuleManager.bHop.setRotation = true;
            }
            else {
                ModuleManager.bHop.setRotation = false;
            }
        }
        if (ModuleManager.bHop.rotateYawOption.isToggled()) {
            if (ModuleManager.bHop.setRotation) {
                if (KillAura.attackingEntity == null && !Utils.noSlowingBackWithBow()) {
                    float playerYaw = mc.thePlayer.rotationYaw;
                    e.setYaw(playerYaw - 55);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }
}
