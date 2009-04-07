//$Id$
/**
 * JavaDoc copy/pastle from the Apache Lucene project
 * Available under the ASL 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a property as indexable
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD } )
@Documented
public @interface Field {
	/**
	 * Field name, default to the JavaBean property name
	 */
	String name() default "";

	/**
	 * Should the value be stored in the document
	 * defaults to no.
	 */
	Store store() default Store.NO;

	/**
	 * Defines how the Field should be indexed
	 * defaults to tokenized
	 */
	Index index() default Index.TOKENIZED;

	/**
	 * Define term vector storage requirements,
	 * default to NO.
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * Define an analyzer for the field, default to
	 * the inherited analyzer
	 */
	Analyzer analyzer() default @Analyzer;


	/**
	 * Boost factor, default 1
	 */
	Boost boost() default @Boost( value = 1.0F );

	/**
	 * Field bridge used. Default is autowired.
	 */
	FieldBridge bridge() default @FieldBridge;
}
