/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

/**
 * A context to configure the document ID mapped to a POJO property.
 */
public interface PropertyDocumentIdMappingContext extends PropertyMappingContext {

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyDocumentIdMappingContext identifierBridge(Class<? extends IdentifierBridge<?>> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyDocumentIdMappingContext identifierBridge(BeanReference<? extends IdentifierBridge<?>> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 * @see IdentifierBridge
	 */
	PropertyDocumentIdMappingContext identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder);

}
