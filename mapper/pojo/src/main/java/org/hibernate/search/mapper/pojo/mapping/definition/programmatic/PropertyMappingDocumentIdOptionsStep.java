/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

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
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyMappingDocumentIdOptionsStep identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder);

}
