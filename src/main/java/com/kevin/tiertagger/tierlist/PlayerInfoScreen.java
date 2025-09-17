package com.kevin.tiertagger.tierlist;

import com.kevin.tiertagger.TierTagger;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.uku3lig.ukulib.config.screen.CloseableScreen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Setter
public class PlayerInfoScreen extends CloseableScreen {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private String player;
    private Identifier texture;
    private boolean everythingIsAwesome = true;

    public PlayerInfoScreen(Screen parent, String player) {
        super(Text.of("Player Info"), parent);
        this.player = player;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());

        this.fetchTexture(this.player).thenAccept(this::setTexture);

        // Hardcoded tier map
        Map<String, String> manualTiers = Map.ofEntries(
            Map.entry("alphaboy", "LT4"),
            Map.entry("15acorn", "LT5"),
            Map.entry("frite211", "HT5"),
            Map.entry("godology", "HT5"),
            Map.entry("itouchedasheep", "HT6"),
            Map.entry("gammer_boy_2008", "LT5"),
            Map.entry("dtoops095", "LT5"),
            Map.entry("f1shs_", "LT5"),
            Map.entry("unfuser", "HT5"),
            Map.entry("searz", "HT5"),
            Map.entry("superbearyo", "LT5"),
            Map.entry("uitimategaming", "LT5")
        );

        String tier = manualTiers.getOrDefault(this.player.toLowerCase(), "Unranked");

        TextWidget modeWidget = new TextWidget(Text.literal("Gamemode: Sword").formatted(Formatting.GRAY), this.textRenderer);
        modeWidget.setX(this.width / 2 + 5);
        modeWidget.setY(this.height / 2 - 15);
        this.addDrawableChild(modeWidget);

        TextWidget tierWidget = new TextWidget(Text.literal("Tier: " + tier).formatted(Formatting.GOLD), this.textRenderer);
        tierWidget.setX(this.width / 2 + 5);
        tierWidget.setY(this.height / 2);
        this.addDrawableChild(tierWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.player + "'s profile", this.width / 2, 20, 0xFFFFFF);

        if (this.texture != null) {
            context.drawTexture(texture, this.width / 2 - 65, (this.height - 144) / 2, 0, 0, 60, 144, 60, 144);

            // Optional: draw sword icon if you add it to your resources
            Identifier swordIcon = new Identifier("tiertagger", "textures/gui/sword.png");
            context.drawTexture(swordIcon, this.width / 2 - 100, this.height / 2 - 20, 0, 0, 16, 16, 16, 16);
        } else {
            String text = this.everythingIsAwesome ? "Loading..." : "Unknown player";
            context.drawCenteredTextWithShadow(this.textRenderer, text, this.width / 2, this.height / 2, 0xFFFFFF);
        }
    }

    private boolean textureExists(Identifier texture) {
        return MinecraftClient.getInstance().getTextureManager().getOrDefault(texture, null) != null;
    }

    private CompletableFuture<Identifier> fetchTexture(String user) {
        String username = user.toLowerCase();
        Identifier tex = Identifier.of(TierTagger.MOD_ID, "player_" + username);

        if (textureExists(tex)) return CompletableFuture.completedFuture(tex);

        TextureManager texManager = MinecraftClient.getInstance().getTextureManager();
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://mc-heads.net/body/" + username + "/240")).GET().build();

        return HTTP_CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenApply(r -> {
            if (r.statusCode() == 200) {
                try {
                    NativeImage image = NativeImage.read(r.body());
                    texManager.registerTexture(tex, new NativeImageBackedTexture(image));
                } catch (IOException e) {
                    TierTagger.getLogger().error("Failed to register head texture", e);
                }
            } else {
                TierTagger.getLogger().error("Could not fetch head texture: {} {}", r.statusCode(), new String(r.body()));
            }

            return tex;
        });
    }
}
