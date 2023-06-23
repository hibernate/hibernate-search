/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.KeywordFieldProcessor;

/**
 * Maps a property to a keyword field in the index, holding a single token (word) of text.
 * <p>
 * On contrary to {@link FullTextField}, this annotation only creates non-tokenized (single-word) text fields.
 * As a result:
 * <ul>
 *     <li>The field value (the value of your annotated property, or at least the value produced by your custom
 *     {@link #valueBridge() value bridge} must be of type String</li>
 *     <li>You cannot assign an analyzer when using this annotation</li>
 *     <li>You can, however, assign a normalizer (which is an analyzer that doesn't perform tokenization)
 *     when using this annotation</li>
 *     <li>This annotation allows to make the field sortable</li>
 * </ul>
 * <p>
 * If you want to index a non-String value, use the {@link GenericField} annotation instead.
 * If you want to index a String value, but want the field to be tokenized, use {@link FullTextField} instead.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(KeywordField.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = KeywordFieldProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface KeywordField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return A reference to the normalizer to use for this field.
	 * Defaults to an empty string, meaning no normalization at all.
	 * See the documentation of your backend to know how to define normalization.
	 */
	String normalizer() default "";

	/**
	 * @return Whether index time scoring information should be stored or not.
	 * @see Norms
	 */
	Norms norms() default Norms.DEFAULT;

	/**
	 * @return Whether projections are enabled for this field.
	 * @see GenericField#projectable()
	 * @see Projectable
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Whether this field should be sortable.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return Whether this field should be searchable.
	 * @see GenericField#searchable()
	 * @see Searchable
	 */
	Searchable searchable() default Searchable.DEFAULT;

	/**
	 * @return Whether aggregations are enabled for this field.
	 * @see GenericField#aggregable()
	 * @see Aggregable
	 */
	Aggregable aggregable() default Aggregable.DEFAULT;

	/**
	 * @return A value used instead of null values when indexing.
	 * @see GenericField#indexNullAs()
	 */
	String indexNullAs() default AnnotationDefaultValues.DO_NOT_INDEX_NULL;

	/**
	 * @return A reference to the value bridge to use for this field.
	 * Must not be set if {@link #valueBinder()} is set.
	 * @see GenericField#valueBridge()
	 * @see ValueBridgeRef
	 */
	ValueBridgeRef valueBridge() default @ValueBridgeRef;

	/**
	 * @return A reference to the value binder to use for this field.
	 * Must not be set if {@link #valueBridge()} is set.
	 * @see GenericField#valueBinder()
	 * @see ValueBinderRef
	 */
	ValueBinderRef valueBinder() default @ValueBinderRef;

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the binding of a value bridge to container elements.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see GenericField#extraction()
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		KeywordField[] value();
	}

}
