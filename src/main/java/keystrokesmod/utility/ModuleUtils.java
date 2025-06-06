package keystrokesmod.utility;

import keystrokesmod.event.*;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.utility.command.CommandManager;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ModuleUtils {
    private final Minecraft mc;

    public ModuleUtils(Minecraft mc) {
        this.mc = mc;
    }

    public static boolean isBreaking;
    public static boolean threwFireball, threwFireballLow;
    private int isBreakingTick;
    public static long MAX_EXPLOSION_DIST_SQ = 10;
    private long FIREBALL_TIMEOUT = 500L, fireballTime = 0;
    public static int inAirTicks, groundTicks, stillTicks;
    public static int fadeEdge;
    public static double offsetValue = 4e-14;
    public static boolean isAttacking;
    private int attackingTicks;
    private int unTargetTicks;
    public static int profileTicks = -1, swapTick;
    public static int lastY, thisY;
    public static boolean lastTickOnGround, lastTickPos1, lastYDif;
    private boolean thisTickOnGround, thisTickPos1;
    public static boolean firstDamage;

    public static boolean isBlocked;

    public static boolean damage;
    private int damageTicks;
    private boolean lowhopAir;

    private int edgeTick;

    private boolean dontCheckFD;

    public static boolean canSlow, didSlow, setSlow, hasSlowed;
    private static boolean allowFriction;

    private float yaw;

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            ModuleManager.disabler.disablerLoaded = false;
            inAirTicks = 0;
        }
    }

    @SubscribeEvent
    public void onSendPacketAll(SendAllPacketsEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof C07PacketPlayerDigging && isBlocked) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging) packet;
            if (Objects.equals(String.valueOf(c07.getStatus()), "RELEASE_USE_ITEM")) {
                isBlocked = false;
            }
        }
        if (packet instanceof C09PacketHeldItemChange && isBlocked) {
            isBlocked = false;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement && Utils.holdingSword() && !BlockUtils.isInteractable(mc.objectMouseOver) && !isBlocked) {
            isBlocked = true;
        }
        if (packet instanceof C02PacketUseEntity) {
            isAttacking = true;
            attackingTicks = 5;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (e.getPacket() instanceof C09PacketHeldItemChange) {
            swapTick = 2;
        }






        if (e.getPacket() instanceof C07PacketPlayerDigging) {
            isBreaking = true;
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball()) {
            if (Utils.keybinds.isMouseDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
                if (mc.thePlayer.rotationPitch > 50F) {
                    threwFireballLow = true;
                }
            }
        }

    }

    @SubscribeEvent
    public void onReceivePacketAll(ReceiveAllPacketsEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.isCanceled()) {
            return;
        }
        if (e.getPacket() instanceof S27PacketExplosion) {
            firstDamage = false;

            dontCheckFD = true;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {

                damage = true;
                damageTicks = 0;

                if (!dontCheckFD) {
                    firstDamage = true;
                }
                dontCheckFD = false;

            }
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (bhopBoostConditions()) {
            if (firstDamage) {
                Utils.setSpeed(Utils.getHorizontalSpeed());
                firstDamage = false;
            }
        }
        if (veloBoostConditions()) {
            if (firstDamage) {
                double added = 0;
                if (Utils.getHorizontalSpeed() <= Velocity.minExtraSpeed.getInput()) {
                    added = Velocity.extraSpeedBoost.getInput() / 100;
                    if (Velocity.reverseDebug.isToggled()) {
                        Utils.print("&7[&dR&7] Applied extra boost | Original speed: " + Utils.getHorizontalSpeed());
                    }
                }
                Utils.setSpeed((Utils.getHorizontalSpeed() * (Velocity.reverseHorizontal.getInput() / 100)) * (1 + added));
                firstDamage = false;
            }
        }
    }

    private boolean bhopBoostConditions() {
        if (ModuleManager.bhop.isEnabled() && ModuleManager.bhop.damageBoost.isToggled() && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
            return true;
        }
        return false;
    }

    private boolean veloBoostConditions() {
        if (ModuleManager.velocity.isEnabled() && ModuleManager.velocity.velocityModes.getInput() == 2) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        double ed = Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX));
        //Utils.print("" + ed);

        if (swapTick > 0) {
            --swapTick;
        }

        if (canSlow || ModuleManager.scaffold.moduleEnabled && !ModuleManager.tower.canTower()) {
            double motionVal = 0.9507832 - ((double) inAirTicks / 10000) - Utils.randomizeDouble(0.00001, 0.00006);
            if (!hasSlowed) motionVal = motionVal - 0.15;
            if (mc.thePlayer.hurtTime == 0 && !setSlow && !mc.thePlayer.onGround) {
                //mc.thePlayer.motionX *= motionVal;
                //mc.thePlayer.motionZ *= motionVal;
                setSlow = hasSlowed = true;
                //Utils.print("Slow " + motionVal);
            }
            didSlow = true;
            //Utils.print(mc.thePlayer.ticksExisted + " : " + Utils.getHorizontalSpeed());
        }
        if (didSlow && mc.thePlayer.onGround) {
            canSlow = didSlow = false;
        }
        if (groundTicks > 1) {
            hasSlowed = false;
        }
        if (mc.thePlayer.onGround || mc.thePlayer.hurtTime != 0) {
            setSlow = false;
        }

        if (!ModuleManager.bhop.running && !ModuleManager.scaffold.fastScaffoldKeepY) {
            allowFriction = false;
        }
        else if (!mc.thePlayer.onGround) {
            allowFriction = true;
        }

        if (damage && ++damageTicks >= 8) {
            damage = firstDamage = false;
            damageTicks = 0;
        }

        profileTicks++;

        if (isAttacking) {
            if (attackingTicks <= 0) {
                isAttacking = false;
            }
            else {
                --attackingTicks;
            }
        }

        if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
            LongJump.stopModules = false;
            LongJump.slotResetTicks = 0;
            LongJump.slotReset = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT / 3) {
            threwFireballLow = false;
            ModuleManager.velocity.disableVelo = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = threwFireballLow = false;
            fireballTime = 0;
            ModuleManager.velocity.disableVelo = false;
        }

        if (isBreaking && ++isBreakingTick >= 1) {
            isBreaking = false;
            isBreakingTick = 0;
        }

        if (ModuleManager.killAura.justUnTargeted) {
            if (++unTargetTicks >= 2) {
                unTargetTicks = 0;
                ModuleManager.killAura.justUnTargeted = false;
            }
        }

        if (CommandManager.status.cooldown != 0) {
            if (mc.thePlayer.ticksExisted % 20 == 0) {
                CommandManager.status.cooldown--;
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        int simpleY = (int) Math.round((e.posY % 1) * 10000);

        if (ModuleManager.scaffold.offsetDelay > 0) {
            --ModuleManager.scaffold.offsetDelay;
        }

        lastTickOnGround = thisTickOnGround;
        thisTickOnGround = mc.thePlayer.onGround;

        lastTickPos1 = thisTickPos1;
        thisTickPos1 = mc.thePlayer.posY % 1 == 0;

        lastY = thisY;
        thisY = (int) mc.thePlayer.posY;

        if (thisY >= lastY + 2 || thisY <= lastY - 2) {
            lastYDif = true;
        }
        else {
            lastYDif = false;
        }

        inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        groundTicks = !mc.thePlayer.onGround ? 0 : ++groundTicks;
        stillTicks = Utils.isMoving() ? 0 : ++stillTicks;

        Block blockAbove = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ));
        Block blockBelow = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ));
        Block blockBelow2 = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 2, mc.thePlayer.posZ));
        Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));


        if ((ModuleManager.bhop.didMove || ModuleManager.scaffold.lowhop) && (!ModuleManager.bhop.disablerOnly.isToggled() || ModuleManager.bhop.disablerOnly.isToggled() && ModuleManager.disabler.disablerLoaded)) {
            if (ModuleUtils.damage && Velocity.vertical.getInput() != 0 || block instanceof BlockSlab) {
                resetLowhop();
            }
            if (!ModuleUtils.damage || Velocity.vertical.getInput() == 0) {

                if (ModuleManager.scaffold.lowhop) {
                    switch (simpleY) {
                        case 4200:
                            mc.thePlayer.motionY = 0.39;
                            break;
                        case 1138:
                            mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                            break;
                        case 2031:
                            mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                            resetLowhop();
                            break;
                    }
                }
                else if (ModuleManager.bhop.didMove) {
                    if (mc.thePlayer.isCollidedVertically || ModuleUtils.damage && Velocity.vertical.getInput() != 0) {// || !ModuleManager.bhop.lowhop && (!(block instanceof BlockAir) || !(blockAbove instanceof BlockAir) || blockBelow instanceof BlockSlab || (blockBelow instanceof BlockAir && blockBelow2 instanceof BlockAir))) {
                        resetLowhop();
                    }
                    switch ((int) ModuleManager.bhop.mode.getInput()) {
                        case 2: // 9 tick
                            switch (simpleY) {
                                case 13:
                                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483;
                                    ModuleManager.bhop.lowhop = true;
                                    break;
                                case 2000:
                                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913;
                                    break;
                                case 7016:
                                    mc.thePlayer.motionY = mc.thePlayer.motionY + 0.08;
                                    break;
                            }
                            if (ModuleUtils.inAirTicks >= 7 && Utils.isMoving()) {
                                Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                            }
                            if (ModuleUtils.inAirTicks >= 9) {
                                resetLowhop();
                            }
                            break;
                        case 3: // 8 tick
                            if (!ModuleManager.bhop.isNormalPos || (block instanceof BlockStairs)) {
                                resetLowhop();
                                break;
                            }
                            boolean g1 = Utils.distanceToGround(mc.thePlayer) <= 1.3;
                            if (inAirTicks == 1) {
                                mc.thePlayer.motionY = 0.38999998569488;
                                ModuleManager.bhop.lowhop = true;
                            }
                            if (inAirTicks == 2) {
                                mc.thePlayer.motionY = 0.30379999189377;
                            }
                            if (inAirTicks == 3) {
                                mc.thePlayer.motionY = 0.08842400075912;
                            }
                            if (inAirTicks == 4) {
                                mc.thePlayer.motionY = -0.19174457909538;
                            }
                            if (inAirTicks == 5) {
                                mc.thePlayer.motionY = -0.26630949469659;
                            }
                            if (inAirTicks == 6) {
                                mc.thePlayer.motionY = -0.26438340940798;
                            }
                            if (inAirTicks == 7 && g1) {
                                mc.thePlayer.motionY = -0.33749574778843;
                            }
                            //strafe
                            if (inAirTicks >= 6 && Utils.isMoving()) {
                                Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                            }
                            //disable
                            if (inAirTicks >= 8 || inAirTicks >= 4 && !g1) {
                                resetLowhop();
                            }
                            break;
                        case 4: // 7 tick
                            switch (simpleY) {
                                case 4200:
                                    mc.thePlayer.motionY = 0.39;
                                    ModuleManager.bhop.lowhop = true;
                                    break;
                                case 1138:
                                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                                    break;
                                case 2031:
                                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                                    resetLowhop();
                                    break;
                            }
                            break;
                    }
                }
            }
        }
        if (!mc.thePlayer.onGround) {
            lowhopAir = true;
        }
        else if (lowhopAir) {
            resetLowhop();
            if (!ModuleManager.bhop.isEnabled()) {
                ModuleManager.bhop.isNormalPos = false;
            }
        }

        if (ModuleManager.bhop.setRotation) {
            if (!ModuleManager.killAura.isTargeting && !ModuleManager.scaffold.isEnabled) {
                yaw = ModuleManager.scaffold.getMotionYaw() - 130.625F * Math.signum(
                        MathHelper.wrapAngleTo180_float(ModuleManager.scaffold.getMotionYaw() - yaw)
                );
                e.setYaw(yaw);
                RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            }
            if (mc.thePlayer.onGround) {
                ModuleManager.bhop.setRotation = false;
            }
        }

        if (ModuleManager.scaffold.canBlockFade && !ModuleManager.scaffold.isEnabled && ++fadeEdge >= 45) {
            ModuleManager.scaffold.canBlockFade = false;
            fadeEdge = 0;
            ModuleManager.scaffold.highlight.clear();
        }
    }

    private void resetLowhop() {
        ModuleManager.bhop.lowhop = ModuleManager.scaffold.lowhop = false;
        ModuleManager.bhop.didMove = false;
        lowhopAir = false;
        edgeTick = 0;
    }

    public static void handleSlow() {
        didSlow = false;
        canSlow = true;
    }

    public static double applyFrictionMulti() {
        final int speedAmplifier = Utils.getSpeedAmplifier();
        if (speedAmplifier > 1 && allowFriction) {
            return Bhop.friction.getInput();
        }
        return 1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!ModuleManager.scaffold.canBlockFade) {
            return;
        }
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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        String stripped = Utils.stripColor(e.message.getUnformattedText());

        //online
        if (stripped.contains("You tipped ") && stripped.contains(" in") && stripped.contains("!") && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.print("§a " + CommandManager.status.ign + " is online");
            e.setCanceled(true);
        }
        if ((stripped.contains("You've already tipped someone in the past hour in") && stripped.contains("! Wait a bit and try again!") || stripped.contains("You've already tipped that person today in ")) && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.print("§a " + CommandManager.status.ign + " is online");
            //client.print(util.colorSymbol + "7^ if player recently left the server this may be innacurate (rate limited)");
            e.setCanceled(true);
        }
        //offline
        if (stripped.contains("That player is not online, try another user!") && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.print("§7 " + CommandManager.status.ign + " is offline");
            e.setCanceled(true);
        }
        //invalid name
        if (stripped.contains("Can't find a player by the name of '") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.print("§7 " + CommandManager.status.ign + " doesn't exist");
            e.setCanceled(true);
        }
        if (stripped.contains("That's not a valid username!") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.print("§binvalid username");
            e.setCanceled(true);
        }
        //checking urself
        if (stripped.contains("You cannot give yourself tips!") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.print("§a " + CommandManager.status.ign + " is online");
            e.setCanceled(true);
        }
    }
}