//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.METHOD, ElementType.FIELD} )
@Documented
/**
 * Specifies that a property of an entity is a Lucene
 * text field
 * @deprecated use @Field(index=Index.TOKENIZED, store=Store.YES)
 */
@Deprecated
public @interface Text {
	/**
	 * The field name
	 */
	String name() default "";
}
