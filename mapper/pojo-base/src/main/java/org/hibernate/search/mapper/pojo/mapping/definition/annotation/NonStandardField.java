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

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.NonStandardFieldProcessor;

/**
 * Maps a property to a non-standard field in the index.
 * <p>
 * This is for advanced use cases, when defining a field whose type is only supported in a specific backend.
 * <p>
 * If you can use a standard index field type, use the {@link GenericField} annotation instead,
 * or one of the specialized annotations: {@link FullTextField}, {@link KeywordField}, ...
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(NonStandardField.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = NonStandardFieldProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface NonStandardField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return A reference to the value binder to use for this field.
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
		NonStandardField[] value();
	}

}
