package org.hibernate.search.annotations;

import org.hibernate.search.bridge.StringBridge;

import java.lang.annotation.*;

/**
 * This annotation means that document ids will be generated externally and does not need to be
 * contained within the class being indexed.
 * <p />
 * Basically, this means that classes annotated with this will NOT be scanned for {@link org.hibernate.search.annotations.DocumentId} annotated fields.
 * @author Navin Surtani  - navin@surtani.org
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ProvidedId
{

   String name() default "ProvidedId";
   Class<StringBridge> bridge() default StringBridge.class;
}
