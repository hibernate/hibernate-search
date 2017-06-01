/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows a user to apply an implementation
 * class to a Lucene document to manipulate it in any way
 * the user sees fit.
 *
 * @author John Griffin
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ClassBridges.class)
public @interface ClassBridge {
	/**
	 * @return default field name passed to your bridge implementation (see {@link org.hibernate.search.bridge.FieldBridge#set(String, Object, org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)}).
	 * <p><b>Note:</b><br>
	 * You can ignore the passed field name in your bridge implementation and add a field or even fields with different names, however any
	 * analyzer specified via {@link #analyzer()} will only apply for the field name specified with this parameter.
	 */
	String name() default "";

	/**
	 * @return Returns an instance of the {@link Store} enum, indicating whether the value should be stored in the document.
	 *         Defaults to {@code Store.NO}
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Index} enum defining whether the value should be indexed or not. Defaults to {@code Index.YES}
	 */
	Index index() default Index.YES;

	/**
	 * @return Returns a {@code Analyze} enum defining whether the value should be analyzed or not. Defaults to {@code Analyze.YES}
	 */
	Analyze analyze() default Analyze.YES;

	/**
	 * @return Returns a {@code Norms} enum defining whether the norms should be stored in the index or not. Defaults to {@code Norms.YES}
	 */
	Norms norms() default Norms.YES;

	/**
	 * @return Returns an instance of the {@link TermVector} enum defining how and if term vectors should be stored.
	 *         Default is {@code TermVector.NO}
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * @return Returns the analyzer to be used. Defaults to none.
	 * Must be empty if {@link #normalizer()} is used.
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * @return Returns the normalizer to be used. Defaults to none.
	 * Must be empty if {@link #analyzer()} is used.
	 */
	Normalizer normalizer() default @Normalizer;

	/**
	 * @return Returns a {@code Boost} annotation defining a float index time boost value
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return Custom implementation of class bridge
	 */
	Class<?> impl();

	/**
	 * @return Array of {@code Parameter} instances passed to the class specified by {@link #impl} to initialize the class
	 *         bridge
	 */
	Parameter[] params() default { };
}
