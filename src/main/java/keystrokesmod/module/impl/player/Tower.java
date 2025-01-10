package keystrokesmod.module.impl.player;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tower extends Module {
    final private SliderSetting towerMove;
    final private SliderSetting verticalTower;
    final private SliderSetting slowedSpeed;
    final private SliderSetting slowedTicks;
    final private ButtonSetting disableWhileHurt;
    final private String[] towerMoveModes = new String[]{"None", "Vanilla", "Low", "Watchdog"};
    final private String[] verticalTowerModes = new String[]{"None", "Vanilla", "Extra block"};
    private int slowTicks;
    private boolean wasTowering;
    private int offGroundTicks;
    private boolean tower;
    private boolean hasTowered, startedTowerInAir, setLowMotion, firstJump;
    private int cMotionTicks, placeTicks;
    public int dCount;

    //vertical tower
    private boolean aligning, aligned, placed;
    private int firstX;
    public boolean placeExtraBlock;

    public boolean speed;

    public Tower() {
        super("Tower", category.player);
        this.registerSetting(towerMove = new SliderSetting("Tower Move", 0, towerMoveModes));
        this.registerSetting(verticalTower = new SliderSetting("Vertical Tower", 0, verticalTowerModes));
        this.registerSetting(slowedSpeed = new SliderSetting("Slowed speed", 2, 0, 9, 0.1));
        this.registerSetting(slowedTicks = new SliderSetting("Slowed ticks", 1, 0, 20, 1));
        this.registerSetting(disableWhileHurt = new ButtonSetting("Disable while hurt", false));
        this.canBeEnabled = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        int valY = (int) Math.round((e.posY % 1) * 10000);
        offGroundTicks++;
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        }
        if (canTower()) {
            wasTowering = true;
            switch ((int) towerMove.getInput()) {
                case 1:
                    mc.thePlayer.motionY = 0.41965;
                    switch (offGroundTicks) {
                        case 1:
                            mc.thePlayer.motionY = 0.33;
                            break;
                        case 2:
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                            break;
                    }
                    if (offGroundTicks >= 3) {
                        offGroundTicks = 0;
                    }
                case 2:
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.4196;
                    }
                    else {
                        switch (offGroundTicks) {
                            case 3:
                            case 4:
                                mc.thePlayer.motionY = 0;
                                break;
                            case 5:
                                mc.thePlayer.motionY = 0.4191;
                                break;
                            case 6:
                                mc.thePlayer.motionY = 0.3275;
                                break;
                            case 11:
                                mc.thePlayer.motionY = - 0.5;

                        }
                    }
                    break;
                case 3:
                    if (!Utils.keysDown()) {
                        break;
                    }
                    if (e.getPosY() % 1 == 0 && !setLowMotion) {
                        tower = true;
                    }
                    if (tower) {
                        if (valY == 0) {
                            mc.thePlayer.motionY = 0.42f;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                            startedTowerInAir = false;
                        }
                        else if (valY > 4000 && valY < 4300) {
                            mc.thePlayer.motionY = 0.33f;
                            Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                            speed = true;
                            hasTowered = true;
                        }
                        else if (valY > 7000) {
                            if (setLowMotion) {
                                tower = false;
                            }
                            speed = false;
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                        }
                    }
                    else if (setLowMotion) {
                        ++cMotionTicks;
                        if (cMotionTicks == 1) {
                            mc.thePlayer.motionY = 0.04f;
                        }
                        else if (cMotionTicks == 3) {
                            cMotionTicks = 0;
                            setLowMotion = false;
                            tower = true;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.02);
                        }
                    }
                    break;
            }
        }
        else {
            if (wasTowering && slowedTicks.getInput() > 0 && modulesEnabled()) {
                if (slowTicks++ < slowedTicks.getInput()) {
                    Utils.setSpeed(Math.max(slowedSpeed.getInput() * 0.1 - 0.25, 0));
                }
                else {
                    slowTicks = 0;
                    wasTowering = false;
                }
            }
            else {
                if (wasTowering) {
                    wasTowering = false;
                }
                slowTicks = 0;
            }
            if (towerMove.getInput() == 2) {
                hasTowered = tower = firstJump = startedTowerInAir = setLowMotion = speed = false;
                cMotionTicks = placeTicks = 0;
            }
            reset();
        }
        if (canTower() && !Utils.keysDown()) {
            wasTowering = true;
            switch ((int) verticalTower.getInput()) {
                case 1:
                    //lazy
                    break;
                case 2:
                    if (!aligned) {
                        if (mc.thePlayer.onGround) {
                            if (!aligning) firstX = (int) mc.thePlayer.posX;
                            mc.thePlayer.motionX = 0.22;
                            aligning = true;
                        }
                        if (aligning && (int) mc.thePlayer.posX > firstX) {
                            aligned = true;
                        }
                        e.setYaw(90F);
                        e.setPitch(85F);
                    }
                    if (aligned) {
                        if (placed) {
                            e.setYaw(270F);
                            e.setPitch(89.9F);
                        }
                        else {
                            e.setYaw(90F);
                            e.setPitch(85F);
                        }
                        placeExtraBlock = true;
                        mc.thePlayer.motionX = 0;
                        mc.thePlayer.motionY = verticalTowerValue();
                        mc.thePlayer.motionZ = 0;
                    }
                    break;
            }
        } else {
            aligning = aligned = placed = false;
            firstX = 0;
            placeExtraBlock = false;
        }
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (canTower() && Utils.keysDown() && towerMove.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
            if (!firstJump) {
                if (!mc.thePlayer.onGround) {
                    if (!startedTowerInAir) {
                        Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.04);
                    }
                    startedTowerInAir = true;
                }
                else if (mc.thePlayer.onGround) {
                    Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                    firstJump = true;
                }
            }
        }
        if (canTower() && !Utils.keysDown() && verticalTower.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (canTower() && Utils.isMoving()) {
                ++placeTicks;
                if (((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection() == 1 && placeTicks > 5 && hasTowered) {
                    dCount++;
                    if (dCount >= 2) {
                        //Utils.sendMessage("Hey");
                        setLowMotion = true;
                    }
                }
                else {
                    dCount = 0;
                }
            }
            else {
                placeTicks = 0;
            }

            if (aligned) {
                placed = true;
            }
            //Utils.print("" + ((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection());
        }
    }

    private void reset() {
        offGroundTicks = 0;
        tower = false;
        placeTicks = 0;
        setLowMotion = false;
    }

    public boolean canTower() {
        if (!Utils.nullCheck() || !Utils.jumpDown()) {
            return false;
        }
        else if (disableWhileHurt.isToggled() && mc.thePlayer.hurtTime > 0) {
            return false;
        }
        else if (mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        else if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava())) {
            return false;
        }
        else if (modulesEnabled()) {
            return true;
        }
        return false;
    }

    private boolean modulesEnabled() {
        return (ModuleManager.scaffold.moduleEnabled && ModuleManager.scaffold.holdingBlocks() && ModuleManager.scaffold.tower.isToggled() && ModuleManager.scaffold.hasSwapped);
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    private double verticalTowerValue() {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        double value = 0;
        if (valY == 0) {
            value = 0.42f;
        } else if (valY > 4000 && valY < 4300) {
            value = 0.33f;
        } else if (valY > 7000) {
            value = 1 - mc.thePlayer.posY % 1f;
        }
        return value;
    }

    private double[] towerSpeedLevels = {0.3, 0.34, 0.38, 0.42, 0.42};

    private double getTowerSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return towerSpeedLevels[speedLevel];
        }
        return towerSpeedLevels[0];
    }

    private double[] towerGroundSpeedLevels = {0.22, 0.25, 0.3, 0.35, 0.4};

    private double getTowerGroundSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return towerGroundSpeedLevels[speedLevel];
        }
        return towerGroundSpeedLevels[0];
    }
}