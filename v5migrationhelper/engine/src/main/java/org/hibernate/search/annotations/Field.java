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

import org.hibernate.search.annotations.impl.FieldAnnotationProcessor;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;

/**
 * Annotation used for marking a property as indexable.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @deprecated Use Hibernate Search 6's field annotations instead:
 * <ul>
 *     <li>{@link FullTextField} for text fields with an analyzer.</li>
 *     <li>{@link KeywordField} for text fields with a normalizer.</li>
 *     <li>{@link GenericField} for non-text fields.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
@Repeatable(Fields.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = FieldAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Field {

	/**
	 * Default value for the {@link #indexNullAs} parameter. Indicates that {@code null} values should not be indexed.
	 */
	String DO_NOT_INDEX_NULL = "__DO_NOT_INDEX_NULL__";

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
	 * @return Returns the value to be used for indexing {@code null}. Per default {@code Field.NO_NULL_INDEXING} is returned indicating that
	 *         null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;
}
