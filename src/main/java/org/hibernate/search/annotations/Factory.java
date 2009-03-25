// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Marks a method as a factory method for a given type.
 * A factory method is called whenever a new instance of a given
 * type is requested.
 * The factory method is used with a higher priority than a plain no-arg constructor when present
 * <br />
 * <code>&#64;Factory</code> currently works for &#64;FullTextFilterDef.impl classes
 * @see org.hibernate.search.annotations.Factory
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface Factory {
}
