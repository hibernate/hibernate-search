/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface PropertyMappingFieldOptionsStep<S extends PropertyMappingFieldOptionsStep<?>>
		extends PropertyMappingStep {

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#valueBridge()
	 */
	S valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see GenericField#valueBridge()
	 */
	S valueBridge(BeanReference<? extends ValueBridge<?, ?>> bridgeReference);

	/**
	 * @param bridgeInstance The bridge instance to use.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#valueBridge()
	 */
	default S valueBridge(ValueBridge<?, ?> bridgeInstance) {
		return valueBridge( BeanReference.ofInstance( bridgeInstance ) );
	}

	/**
	 * Define a value binder, responsible for creating a bridge.
	 * To pass some parameters to the bridge,
	 * use the method {@link #valueBinder(ValueBinder, Map)} instead.
	 *
	 * @param binder A {@link ValueBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#valueBinder()
	 * @see ValueBinder
	 */
	default S valueBinder(ValueBinder binder) {
		return valueBinder( binder, Collections.emptyMap() );
	}

	/**
	 * Define a value binder, responsible for creating a bridge.
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binder A {@link ValueBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see ValueBinder
	 */
	S valueBinder(ValueBinder binder, Map<String, Object> params);

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default S extractor(String extractorName) {
		return extractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#extraction()
	 */
	default S noExtractors() {
		return extractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#extraction()
	 * @see ContainerExtractorPath
	 */
	S extractors(ContainerExtractorPath extractorPath);

}
