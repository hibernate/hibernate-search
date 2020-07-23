/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public interface DefaultBridgeDefinitionStep<S extends DefaultBridgeDefinitionStep<?, T>, T>
		extends DefaultBinderDefinitionStep<S> {

	/**
	 * Use the given bridge by default for properties with a matching type marked as document identifier
	 * (e.g. with {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId}).
	 * @param bridge The bridge to apply to matching properties by default.
	 * @return {@code this}, for method chaining.
	 */
	S identifierBridge(IdentifierBridge<T> bridge);

	/**
	 * Use the given bridge by default for properties with a matching type mapped to an index field directly
	 * (e.g. with {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField},
	 * {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField}, ...).
	 * @param bridge The bridge to apply to matching properties by default.
	 * @return {@code this}, for method chaining.
	 */
	S valueBridge(ValueBridge<T, ?> bridge);

}
