package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DelayRemover extends Module {
    public static ButtonSetting oldReg;
    public static SliderSetting removeJumpTicks;
    private String[] removeJumpTicksModes = new String[] { "Disabled", "0", "Hypixel" };

    public DelayRemover() {
        super("Delay Remover", category.player, 0);
        this.registerSetting(oldReg = new ButtonSetting("1.7 hitreg", true));
        this.registerSetting(removeJumpTicks = new SliderSetting("Remove jump ticks", 0, removeJumpTicksModes));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !mc.inGameHasFocus || !Utils.nullCheck()) {
            return;
        }
        if (oldReg.isToggled()) {
            ((IAccessorMinecraft) mc).setLeftClickCounter(0);
        }
        switch ((int) removeJumpTicks.getInput()) {
            case 1:
                ((IAccessorEntityLivingBase) mc.thePlayer).setJumpTicks(0);
                break;
            case 2:
                if (!mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically || Utils.isMoving()) {
                    ((IAccessorEntityLivingBase) mc.thePlayer).setJumpTicks(0);
                }
                break;
        }

    }

    private boolean removeJumpDelay() {
        return !mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically;
    }
}
