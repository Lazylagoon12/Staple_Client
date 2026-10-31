/*
 * This file is part of the Staple Client distribution.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {
    // Leave Minecraft's own splash selection untouched.
    // Staple Client no longer injects Meteor-branded splash messages.
}
