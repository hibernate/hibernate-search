/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

/**
 * @param <E> The entity type mapped to an index.
 */
public final class PojoDocumentReferenceProvider<E> implements DocumentReferenceProvider {

	private final RoutingKeyProvider<E> routingKeyProvider;

	private final AbstractPojoBackendSessionContext sessionContext;
	private final Object identifier;
	private final String documentIdentifier;
	private final Supplier<E> entitySupplier;

	public PojoDocumentReferenceProvider(
			RoutingKeyProvider<E> routingKeyProvider,
			AbstractPojoBackendSessionContext sessionContext,
			Object identifier,
			String documentIdentifier,
			Supplier<E> entitySupplier) {
		this.routingKeyProvider = routingKeyProvider;
		this.sessionContext = sessionContext;
		this.identifier = identifier;
		this.documentIdentifier = documentIdentifier;
		this.entitySupplier = entitySupplier;
	}

	@Override
	public String getIdentifier() {
		return documentIdentifier;
	}

	@Override
	public String getRoutingKey() {
		return routingKeyProvider.toRoutingKey(
				identifier,
				entitySupplier,
				sessionContext
		);
	}

}
