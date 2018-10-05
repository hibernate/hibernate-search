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

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;

/**
 * A keyword field in the full text index, holding a single token (word) of text.
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
	 * @return Whether values for this field should be stored (enables projections).
	 * @see GenericField#store()
	 */
	Store store() default Store.DEFAULT;

	/**
	 * @return Whether this field should be sortable.
	 * @see GenericField#sortable()
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return A reference to the value bridge to use for this field.
	 * @see GenericField#valueBridge()
	 */
	ValueBridgeBeanReference valueBridge() default @ValueBridgeBeanReference;

	/**
	 * @return A reference to the builder to use to build a value bridge for this field.
	 * @see GenericField#valueBridgeBuilder()
	 */
	ValueBridgeBuilderBeanReference valueBridgeBuilder() default @ValueBridgeBuilderBeanReference;

	/**
	 * @return An array of reference to container value extractor implementation classes.
	 * @see GenericField#extractors()
	 */
	ContainerValueExtractorBeanReference[] extractors()
			default @ContainerValueExtractorBeanReference(type = ContainerValueExtractorBeanReference.DefaultExtractors.class);

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		KeywordField[] value();
	}

}
