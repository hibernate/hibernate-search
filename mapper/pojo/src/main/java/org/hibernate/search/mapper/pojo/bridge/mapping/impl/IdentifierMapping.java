/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to an index.
 */
public interface IdentifierMapping<I, E> extends AutoCloseable {

	@Override
	default void close() {
	}

	I getIdentifier(Object providedId);

	I getIdentifier(Object providedId, Supplier<? extends E> entitySupplier);

	String toDocumentIdentifier(I identifier, AbstractPojoMappingContextImplementor context);

	I fromDocumentIdentifier(String documentId, AbstractPojoSessionContextImplementor sessionContext);

}
