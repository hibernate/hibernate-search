/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;

class NotifyingNestingContext implements IndexSchemaNestingContext {
	private final IndexSchemaNestingContext delegate;
	private final IndexSchemaContributionListener listener;

	NotifyingNestingContext(IndexSchemaNestingContext delegate, IndexSchemaContributionListener listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public <T> T nest(String relativeName, LeafFactory<T> factoryIfIncluded, LeafFactory<T> factoryIfExcluded) {
		return delegate.nest(
				relativeName,
				prefixedName -> {
					listener.onSchemaContributed();
					return factoryIfIncluded.create( prefixedName );
				},
				factoryIfExcluded
		);
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factoryIfIncluded,
			CompositeFactory<T> factoryIfExcluded) {
		return delegate.nest(
				relativeName,
				(prefixedName, nestedNestingContext) -> {
					listener.onSchemaContributed();
					return factoryIfIncluded.create(
							prefixedName,
							new NotifyingNestingContext( nestedNestingContext, listener )
					);
				},
				factoryIfExcluded
		);
	}
}
