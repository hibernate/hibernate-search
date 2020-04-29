/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;

class NotifyingNestingContext implements IndexSchemaNestingContext {
	private final IndexSchemaNestingContext delegate;
	private final IndexSchemaContributionListener listener;

	NotifyingNestingContext(IndexSchemaNestingContext delegate, IndexSchemaContributionListener listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public <T> T nest(String relativeName, LeafFactory<T> factory) {
		return delegate.nest(
				relativeName,
				(prefixedName, inclusion) -> {
					if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onSchemaContributed();
					}
					return factory.create( prefixedName, inclusion );
				}
		);
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factory) {
		return delegate.nest(
				relativeName,
				(prefixedName, inclusion, nestedNestingContext) -> {
					if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onSchemaContributed();
					}
					// No need to wrap the nested context:
					// if we're included, the listener was notified;
					// if we're excluded, children will be excluded as well.
					return factory.create( prefixedName, inclusion, nestedNestingContext );
				}
		);
	}

	@Override
	public <T> T nestTemplate(TemplateFactory<T> factory) {
		return delegate.nestTemplate(
				(inclusion, prefix) -> {
					if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onSchemaContributed();
					}
					return factory.create( inclusion, prefix );
				}
		);
	}
}
