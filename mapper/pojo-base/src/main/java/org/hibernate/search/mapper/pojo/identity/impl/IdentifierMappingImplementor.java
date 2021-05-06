/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to an index.
 */
public interface IdentifierMappingImplementor<I, E> extends IdentifierMapping, AutoCloseable {

	@Override
	default void close() {
	}

	@Override
	I fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext);

	I getIdentifier(Object providedId, Supplier<? extends E> entitySupplier);

	String toDocumentIdentifier(I identifier, BridgeMappingContext context);

}
