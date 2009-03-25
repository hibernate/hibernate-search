// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author John Griffin
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ClassBridges {
	/**
	 * An array of ClassBridge annotations each of
	 * which is to be applied to the class containing
	 * this annotation.
	 */
	ClassBridge[] value() default {};
}