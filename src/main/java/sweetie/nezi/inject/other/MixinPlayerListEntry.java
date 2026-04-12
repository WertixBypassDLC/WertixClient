package sweetie.nezi.inject.other;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {
    @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    private SkinTextures skinTexturesHook(SkinTextures original) {
        return original;
    }
}
