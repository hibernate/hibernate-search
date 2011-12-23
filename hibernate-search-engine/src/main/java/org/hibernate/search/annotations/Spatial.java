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
@Target( { ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface Spatial {
	/**
	 * @return minimum grid level for spatial indexing
	 */
	int min_grid_level() default SpatialFieldBridge.MIN_GRID_LEVEL;

	/**
	 * @return minimum grid level for spatial indexing
	 */
	int max_grid_level() default SpatialFieldBridge.MAX_GRID_LEVEL;
}

