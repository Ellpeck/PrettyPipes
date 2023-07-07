package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.PlayerPrefs;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

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
        registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(), RecipeTypes.CRAFTING);
        registration.addUniversalRecipeTransferHandler(new CraftingModuleTransferHandler());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ItemTerminalGui.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ItemTerminalGui containerScreen) {
                List<Rect2i> ret = new ArrayList<>();
                // sorting buttons
                ret.add(new Rect2i(containerScreen.getGuiLeft() - 22, containerScreen.getGuiTop(), 22, 64));
                // crafting hud
                if (containerScreen.currentlyCrafting != null && !containerScreen.currentlyCrafting.isEmpty())
                    ret.add(new Rect2i(containerScreen.getGuiLeft() + containerScreen.getXSize(), containerScreen.getGuiTop() + 4, 65, 89));
                return ret;
            }
        });
    }

    @SubscribeEvent
    public void onInitGui(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof ItemTerminalGui terminal))
            return;
        terminal.addRenderableWidget(this.jeiSyncButton = Button.builder(Component.literal(""), button -> {
            var preferences = PlayerPrefs.get();
            preferences.syncJei = !preferences.syncJei;
            preferences.save();
            terminal.updateWidgets();
        }).bounds(terminal.getGuiLeft() - 22, terminal.getGuiTop() + 44, 20, 20).build());
        if (PlayerPrefs.get().syncJei)
            terminal.search.setValue(this.runtime.getIngredientFilter().getFilterText());
    }

    @SubscribeEvent
    public void onRenderGui(ScreenEvent.Render event) {
        var screen = event.getScreen();
        if (!(screen instanceof ItemTerminalGui terminal))
            return;
        var sync = PlayerPrefs.get().syncJei;
        if (event instanceof ScreenEvent.Render.Post) {
            if (this.jeiSyncButton.isHoveredOrFocused())
                event.getGuiGraphics().renderTooltip(terminal.getMinecraft().font, Component.translatable("info." + PrettyPipes.ID + ".sync_jei." + (sync ? "on" : "off")), event.getMouseX(), event.getMouseY());
        } else if (event instanceof ScreenEvent.Render.Pre) {
            this.jeiSyncButton.setMessage(Component.literal((sync ? ChatFormatting.GREEN : ChatFormatting.RED) + "J"));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!PlayerPrefs.get().syncJei)
            return;

        var screen = Minecraft.getInstance().screen;
        if (!(screen instanceof ItemTerminalGui terminal)) {
            this.lastTerminalText = null;
            this.lastJeiText = null;
            return;
        }
        var filter = this.runtime.getIngredientFilter();
        var terminalText = terminal.search.getValue();
        var jeiText = filter.getFilterText();

        if (!jeiText.equals(this.lastJeiText)) {
            this.lastTerminalText = jeiText;
            this.lastJeiText = jeiText;
            terminal.search.setValue(jeiText);
        } else if (!terminalText.equals(this.lastTerminalText)) {
            this.lastTerminalText = terminalText;
            this.lastJeiText = terminalText;
            filter.setFilterText(terminalText);
        }
    }
}
