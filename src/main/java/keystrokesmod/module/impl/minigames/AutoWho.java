package keystrokesmod.module.impl.minigames;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoWho extends Module {
    private ButtonSetting artifical;
    private ButtonSetting hideMessage;
    private ButtonSetting removeBots;
    private ButtonSetting onGameStart;

    public AutoWho() {
        super("AutoWho", category.minigames);
        this.registerSetting(new DescriptionSetting("Automatically execute /who."));
        this.registerSetting(new DescriptionSetting(Utils.formatColor("Use '&enick [nick]&r' when nicked.")));
        this.registerSetting(artifical = new ButtonSetting("Artificial", false));
        this.registerSetting(hideMessage = new ButtonSetting("Hide message", false));
        this.registerSetting(removeBots = new ButtonSetting("Remove bots", true));
        this.registerSetting(onGameStart = new ButtonSetting("On game start", false));
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Utils.nullCheck()) {
            return;
        }
        final String r = Utils.stripColor(e.message.getUnformattedText());
        if (r.isEmpty()) {
            return;
        }
        if (!onGameStart.isToggled() && (r.replace("!", "").trim().startsWith(Utils.getServerName()) && ((r.contains("(") && r.contains(")")) || r.contains("/"))) || onGameStart.isToggled() && r.contains("Protect your bed and destroy the enemy beds.")) {
            this.artificial();
        }
        else if (hideMessage.isToggled() && r.startsWith("ONLINE: ")) {
            e.setCanceled(true);
            Utils.log.info("[CHAT] " + r);
        }
    }

    private void artificial() {
        if (artifical.isToggled()) {
            String online = hideMessage.isToggled() ? "ONLINE: " : "&b&lONLINE: &r";
            for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(true)) {
                if (removeBots.isToggled() && networkPlayerInfo.getResponseTime() > 1) {
                    continue;
                }
                if (hideMessage.isToggled()) {
                    online = online + networkPlayerInfo.getGameProfile().getName() + ", ";
                } else {
                    online = online + ScorePlayerTeam.formatPlayerName(networkPlayerInfo.getPlayerTeam(), networkPlayerInfo.getGameProfile().getName()) + "�" + "7, ";
                }
            }
            if (hideMessage.isToggled()) {
                Utils.log.info("[CHAT] " + (online + mc.thePlayer.getName()));
                return;
            }
            Utils.sendRawMessage(online + mc.thePlayer.getDisplayName().getFormattedText());
        } else {
            mc.thePlayer.sendChatMessage("/who");
        }
    }
}
