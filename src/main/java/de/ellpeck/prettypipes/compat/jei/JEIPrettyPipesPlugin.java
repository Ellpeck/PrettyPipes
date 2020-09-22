package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.PlayerPrefs;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

@JeiPlugin
public class JEIPrettyPipesPlugin implements IModPlugin {

    private IJeiRuntime runtime;
    private String lastTerminalText;
    private String lastJeiText;
    private Button jeiSyncButton;

    public JEIPrettyPipesPlugin() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(PrettyPipes.ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(), VanillaRecipeCategoryUid.CRAFTING);
    }

    @SubscribeEvent
    public void onInitGui(InitGuiEvent.Post event) {
        Screen screen = event.getGui();
        if (!(screen instanceof ItemTerminalGui))
            return;
        ItemTerminalGui terminal = (ItemTerminalGui) screen;
        event.addWidget(this.jeiSyncButton = new Button(terminal.getGuiLeft() - 22, terminal.getGuiTop() + 44, 20, 20, new StringTextComponent(""), button -> {
            PlayerPrefs prefs = PlayerPrefs.get();
            prefs.syncJei = !prefs.syncJei;
            prefs.save();
            terminal.updateWidgets();
        }));
        if (PlayerPrefs.get().syncJei)
            terminal.search.setText(this.runtime.getIngredientFilter().getFilterText());
    }

    @SubscribeEvent
    public void onRenderGui(DrawScreenEvent event) {
        Screen screen = event.getGui();
        if (!(screen instanceof ItemTerminalGui))
            return;
        ItemTerminalGui terminal = (ItemTerminalGui) screen;
        boolean sync = PlayerPrefs.get().syncJei;
        if (event instanceof DrawScreenEvent.Post) {
            if (this.jeiSyncButton.isHovered())
                terminal.renderTooltip(event.getMatrixStack(), new TranslationTextComponent("info." + PrettyPipes.ID + ".sync_jei." + (sync ? "on" : "off")), event.getMouseX(), event.getMouseY());
        } else if (event instanceof DrawScreenEvent.Pre) {
            this.jeiSyncButton.setMessage(new StringTextComponent((sync ? TextFormatting.GREEN : TextFormatting.RED) + "J"));
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (!PlayerPrefs.get().syncJei)
            return;

        Screen screen = Minecraft.getInstance().currentScreen;
        if (!(screen instanceof ItemTerminalGui)) {
            this.lastTerminalText = null;
            this.lastJeiText = null;
            return;
        }
        ItemTerminalGui terminal = (ItemTerminalGui) screen;
        IIngredientFilter filter = this.runtime.getIngredientFilter();
        String terminalText = terminal.search.getText();
        String jeiText = filter.getFilterText();

        if (!jeiText.equals(this.lastJeiText)) {
            this.lastTerminalText = jeiText;
            this.lastJeiText = jeiText;
            terminal.search.setText(jeiText);
        } else if (!terminalText.equals(this.lastTerminalText)) {
            this.lastTerminalText = terminalText;
            this.lastJeiText = terminalText;
            filter.setFilterText(terminalText);
        }
    }
}
