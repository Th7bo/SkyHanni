//? if < 26.1 {
/*package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.VisualWordsHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @WrapOperation(
        method = "addMessageToDisplayQueue",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/GuiMessage$Line"
        )
    )
    private GuiMessage.Line addMessageId(
        int addedTime,
        FormattedCharSequence content,
        GuiMessageTag tag,
        boolean endOfEntry,
        Operation<GuiMessage.Line> original,
        GuiMessage message
    ) {
        FormattedCharSequence transformedContent = VisualWordsHook.modifyOrderedText(content);
        GuiMessage.Line line = original.call(addedTime, transformedContent, tag, endOfEntry);
        line.skyhanni$setParent(message);
        return line;
    }
}
*///?}
