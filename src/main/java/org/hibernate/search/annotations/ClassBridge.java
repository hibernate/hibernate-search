// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows a user to apply an implementation
 * class to a Lucene document to manipulate it in any way
 * the user sees fit.
 *
 * @author John Griffin
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ClassBridge {
	/**
	 * Field name, default to the JavaBean property name.
	 */
	String name() default "";

	/**
	 * Should the value be stored in the document.
	 * defaults to no.
	 */
	Store store() default Store.NO;

	/**
	 * Define an analyzer for the field, default to
	 * the inherited analyzer.
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * Defines how the Field should be indexed
	 * defaults to tokenized.
	 */
	Index index() default Index.TOKENIZED;

	/**
	 * Define term vector storage requirements,
	 * default to NO.
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * A float value of the amount of Lucene defined
	 * boost to apply to a field.
	 */
	Boost boost() default @Boost(value=1.0F);

	/**
	 * User supplied class to manipulate document in
	 * whatever mysterious ways they wish to.
	 */
	public Class<?> impl();

	/**
	 * Array of fields to work with. The impl class
	 * above will work on these fields.
	 */
	public Parameter[] params() default {};

}