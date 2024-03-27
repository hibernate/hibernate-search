/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;

/**
 * The step in a property-to-document-ID mapping where optional parameters can be set.
 */
public interface PropertyMappingDocumentIdOptionsStep extends PropertyMappingStep {

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyMappingDocumentIdOptionsStep identifierBridge(Class<? extends IdentifierBridge<?>> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyMappingDocumentIdOptionsStep identifierBridge(BeanReference<? extends IdentifierBridge<?>> bridgeReference);

	/**
	 * @param bridgeInstance A bridge instance to use.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	default PropertyMappingDocumentIdOptionsStep identifierBridge(IdentifierBridge<?> bridgeInstance) {
		return identifierBridge( BeanReference.ofInstance( bridgeInstance ) );
	}

	/**
	 * Define an identifier binder, responsible for creating a bridge.
	 * To pass some parameters to the bridge,
	 * use the method {@link #identifierBinder(IdentifierBinder, Map)} instead.
	 *
	 * @param binder A {@link IdentifierBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBinder
	 */
	default PropertyMappingDocumentIdOptionsStep identifierBinder(IdentifierBinder binder) {
		return identifierBinder( binder, Collections.emptyMap() );
	}

	/**
	 * Define an identifier binder, responsible for creating a bridge.
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binder A {@link IdentifierBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBinder
	 */
	PropertyMappingDocumentIdOptionsStep identifierBinder(IdentifierBinder binder, Map<String, Object> params);

}
