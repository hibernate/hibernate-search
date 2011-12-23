package org.hibernate.search.annotations;

import org.hibernate.search.spatial.SpatialFieldBridge;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for {@code @Field} for Hibernate Search Spatial Index
 *
 * @author Nicolas Helleringer (nicolas.helleringer@novacodex.net)

 * @experimental Spatial indexing in Hibernate Search is still in its first drafts
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
public @interface Spatial {
	/**
	 * @return the field name (defaults to the JavaBean property name)
	 */
	String name() default "";

	/**
	 * @return Returns an instance of the {@link Store} enum, indicating whether the value should be stored in the document.
	 *         Defaults to {@code Store.NO}
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Boost} annotation defining a float index time boost value
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return minimum grid level for spatial indexing
	 */
	int highGridLevel() default SpatialFieldBridge.MIN_GRID_LEVEL;

	/**
	 * @return minimum grid level for spatial indexing
	 */
	int lowGridLevel() default SpatialFieldBridge.MAX_GRID_LEVEL;
}

