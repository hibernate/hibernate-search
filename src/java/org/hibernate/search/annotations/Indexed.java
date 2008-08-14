//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.*;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
/**
 * Specifies that an entity is to be indexed by Lucene
 */
public @interface Indexed {
	/**
	 * The filename of the index
	 */
	String index() default "";
}
