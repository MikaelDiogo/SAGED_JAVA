package br.gov.crateus.bcm.sdk.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a business module for platform registry and documentation.
 * Place on {@code package-info.java} or a module configuration type.
 * Example: {@code @BcmBusinessModule(id = "inventory", displayName = "Inventory")}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface BcmBusinessModule {

	/** Stable id (English), matches BDM schema and {@code bcm.modules.<id>} flags. */
	String id();

	String displayName() default "";
}
