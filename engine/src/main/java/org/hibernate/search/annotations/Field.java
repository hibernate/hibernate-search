/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * JavaDoc copy/pastle from the Apache Lucene project
 * Available under the ASL 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for marking a property as indexable.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Repeatable(Fields.class)
public @interface Field {

	/**
	 * Default value for the {@link #indexNullAs} parameter. Indicates that {@code null} values should not be indexed.
	 */
	String DO_NOT_INDEX_NULL = "__DO_NOT_INDEX_NULL__";

	/**
	 * Value for the {@link #indexNullAs} parameter indicating that {@code null} values should be indexed using the null
	 * token given through the {@link org.hibernate.search.cfg.Environment#DEFAULT_NULL_TOKEN} configuration property.
	 * If no value is given for that property, the token {@code _null_} will be used.
	 */
	String DEFAULT_NULL_TOKEN = "__DEFAULT_NULL_TOKEN__";

	/**
	 * @return Returns the field name. Defaults to the JavaBean property name.
	 */
	String name() default "";

	/**
	 * @return Returns a {@code Store} enum type indicating whether the value should be stored in the document. Defaults to {@code Store.NO}.
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Index} enum defining whether the value should be indexed or not. Defaults to {@code Index.YES}.
	 */
	Index index() default Index.YES;

	/**
	 * @return Returns a {@code Analyze} enum defining whether the value should be analyzed or not. Defaults to {@code Analyze.YES}.
	 */
	Analyze analyze() default Analyze.YES;

	/**
	 * @return Returns a {@code Norms} enum defining whether the norms should be stored in the index or not. Defaults to {@code Norms.YES}.
	 */
	Norms norms() default Norms.YES;

	/**
	 * @return Returns a {@code TermVector} enum defining if and how term vectors are stored. Defaults to {@code TermVector.NO}.
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * @return Returns the analyzer for the field. Defaults to the inherited analyzer.
	 * Must be empty if {@link #normalizer()} is used.
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * @return Returns the normalizer for the field. Defaults to none.
	 * Must be empty if {@link #analyzer()} is used.
	 */
	Normalizer normalizer() default @Normalizer;

	/**
	 * @return Returns the boost factor for the field. Default boost factor is 1.0.
	 *
	 * @deprecated Index-time boosting will not be possible anymore starting from Lucene 7.
	 * You should use query-time boosting instead, for instance by calling
	 * {@link org.hibernate.search.query.dsl.FieldCustomization#boostedTo(float) boostedTo(float)}
	 * when building queries with the Hibernate Search query DSL.
	 */
	@Deprecated
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return Returns the field bridge used for this field. Default is auto-wired.
	 */
	FieldBridge bridge() default @FieldBridge;

	/**
	 * @return Returns the value to be used for indexing {@code null}. Per default {@code Field.NO_NULL_INDEXING} is returned indicating that
	 *         null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;
}
