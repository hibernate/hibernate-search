/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.MappingAnnotationProcessorUtils;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * A superinterface for contexts passed to mapping annotation processors.
 */
public interface MappingAnnotationProcessorContext {

	/**
	 * @return A representation of the annotated element.
	 */
	MappingAnnotatedElement annotatedElement();

	/**
	 * Convert an {@link ObjectPath} annotation to a {@link PojoModelPathValueNode}.
	 * @param objectPath The annotation to convert.
	 * @return The corresponding path, or an empty optional if the path was empty.
	 */
	Optional<PojoModelPathValueNode> toPojoModelPathValueNode(ObjectPath objectPath);

	/**
	 * Convert a {@link ContainerExtraction} annotation to a {@link ContainerExtractorPath}.
	 * @param extraction The annotation to convert.
	 * @return The corresponding path.
	 */
	ContainerExtractorPath toContainerExtractorPath(ContainerExtraction extraction);

	/**
	 * Shorthand for
	 * {@link #toBeanReference(Class, Class, Class, String, BeanRetrieval) toBeanReference(expectedType, undefinedTypeMarker, type, name, BeanRetrieval.ANY)}.
	 *
	 * @param expectedType The supertype of all types that can be referenced.
	 * @param undefinedTypeMarker A marker type to detect that the {@code type} parameter has its default value (undefined).
	 * @param type The bean type.
	 * @param name The bean name.
	 * @param <T> The bean type.
	 * @return The corresponding bean reference, or an empty optional if neither the type nor the name is provided.
	 */
	default <T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name) {
		return toBeanReference( expectedType, undefinedTypeMarker, type, name, BeanRetrieval.ANY );
	}

	/**
	 * Convert attributes of a bean-reference annotation,
	 * such as {@link org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef},
	 * to an actual {@link BeanReference}.
	 * <p>
	 * Example of use:
	 * <pre>{@code
	 *     Optional<BeanReference<? extends ValueBridge>> valueBridgeRef = toBeanReference(
	 *             ValueBridge.class,
	 *             ValueBridgeRef.UndefinedBridgeImplementationType.class,
	 *             myValueBridgeRefInstance.type(),
	 *             myValueBridgeRefInstance.name(),
	 *             myValueBridgeRefInstance.retrieval()
	 *     );
	 * }</pre>
	 *
	 * @param expectedType The supertype of all types that can be referenced.
	 * @param undefinedTypeMarker A marker type to detect that the {@code type} parameter has its default value (undefined).
	 * @param type The bean type.
	 * @param name The bean name.
	 * @param retrieval How to retrieve the bean. See {@link BeanRetrieval}.
	 * @param <T> The bean type.
	 * @return The corresponding bean reference, or an empty optional if neither the type nor the name is provided.
	 */
	<T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name, BeanRetrieval retrieval);

	/**
	 * @return An event context describing the annotation being processed and its location,
	 * for use in log messages and exception messages.
	 */
	EventContext eventContext();

	/**
	 * @param params The original params
	 * @return A {@link Map} created from {@link Param}
	 * using {@link Param#name()} as key and {@link Param#value()} as value
	 * @throws SearchException if {@code params} contain multiple parameters with the same {@link Param#name()}
	 */
	default Map<String, Object> toMap(Param[] params) {
		return MappingAnnotationProcessorUtils.toMap( params );
	}

	/**
	 * @param value A value extracted from an annotation attribute.
	 * @param defaultValue A default value for that annotation attribute.
	 * @return {@code null} if {@code value} is {@link Objects#equals(Object, Object) equal}
	 * to {@code defaultValue}, {@code value} otherwise.
	 */
	<T> T toNullIfDefault(T value, T defaultValue);

}
