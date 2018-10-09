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
 * @author Yoann Rodiere
 */
public interface PropertyDocumentIdMappingContext extends PropertyMappingContext {

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 */
	PropertyDocumentIdMappingContext identifierBridge(Class<? extends IdentifierBridge<?>> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * The bridge must implement {@link IdentifierBridge}.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 */
	PropertyDocumentIdMappingContext identifierBridge(BeanReference bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 */
	PropertyDocumentIdMappingContext identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder);

}
