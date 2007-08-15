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
 * keyword field
 * @deprecated use @Field(index=Index.UN_TOKENIZED, store=Store.YES) or @DocumentId when id=true was used
 */
@Deprecated
public @interface Keyword {
	/**
	 * The field name
	 */
	String name() default "";

	/**
	 * Specifies that this is the "identifier" keyword
	 */
	boolean id() default false;
}
