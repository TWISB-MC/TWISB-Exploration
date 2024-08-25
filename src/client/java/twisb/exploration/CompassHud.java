package twisb.exploration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CompassHud {

    private final MinecraftClient client;
    private final TextRenderer textRenderer;

    public CompassHud(MinecraftClient client) {
        this.client = client;
        this.textRenderer = client.textRenderer;
    }

    public void render(DrawContext context) {
        context.draw(() -> {
            this.drawLeftText(context);
        });
    }

    private void drawText(DrawContext context, List<String> text, boolean left) {
        Objects.requireNonNull(this.textRenderer);
        int i = 9;

        int j;
        String string;
        int k;
        int l;
        int m;
        for(j = 0; j < text.size(); ++j) {
            string = (String)text.get(j);
            if (!Strings.isNullOrEmpty(string)) {
                k = this.textRenderer.getWidth(string);
                l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
                m = 2 + i * j;
                context.fill(l - 1, m - 1, l + k + 1, m + i - 1, -1873784752);
            }
        }

        for(j = 0; j < text.size(); ++j) {
            string = (String)text.get(j);
            if (!Strings.isNullOrEmpty(string)) {
                k = this.textRenderer.getWidth(string);
                l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
                m = 2 + i * j;
                context.drawText(this.textRenderer, string, l, m, 14737632, false);
            }
        }

    }

    protected void drawLeftText(DrawContext context) {
        List<String> list = this.getLeftText();
        this.drawText(context, list, true);
    }

    protected List<String> getLeftText() {
//        String[] strings = new String[0];
        List<String> list = Lists.newArrayList();
        Entity camera = this.client.getCameraEntity();
        if (camera == null) return list;
        list.add(String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", camera.getX(), camera.getY(), camera.getZ()));
        Direction direction = camera.getHorizontalFacing();
        list.add(String.format(Locale.ROOT, "Facing %s", direction));
        return list;
    }
}
