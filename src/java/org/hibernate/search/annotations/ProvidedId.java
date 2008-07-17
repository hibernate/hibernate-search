// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.bridge.StringBridge;

/**
 * Objects whose identifier is provided externally and not part of the object state
 * should be marked with this annotation
 * <p/>
 * This annotation should not be used in conjunction with {@link org.hibernate.search.annotations.DocumentId}
 *
 * @author Navin Surtani  - navin@surtani.org
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ProvidedId {

	String name() default "providedId";

	Class<?> bridge() default StringBridge.class;
}
