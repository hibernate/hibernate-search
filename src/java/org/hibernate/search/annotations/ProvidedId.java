// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Objects whose identifier is provided externally and not part of the object state
 * should be marked with this annotation
 * <p/>
 * This annotation should not be used in conjunction with {@link org.hibernate.search.annotations.DocumentId}
 *
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ProvidedId {

   String name() default "providedId";

   FieldBridge bridge() default @FieldBridge(impl = org.hibernate.search.bridge.builtin.StringBridge.class);
   
}
