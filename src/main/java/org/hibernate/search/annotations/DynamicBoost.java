//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.BoostStrategy;

/**
 * Apply a dynamic boost factor on a field or a whole entity.
 *
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface DynamicBoost {

	/**
	 * @return An implementation of <code>BoostStrategy</code> to apply a boost
	 *         value as function of the annotated object.
	 *
	 * @see org.hibernate.search.engine.BoostStrategy
	 */
	public abstract Class<? extends BoostStrategy> impl();
}