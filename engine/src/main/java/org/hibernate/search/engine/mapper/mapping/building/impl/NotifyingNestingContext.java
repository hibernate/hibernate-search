/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;

class NotifyingNestingContext implements IndexSchemaNestingContext {
	private final IndexSchemaNestingContext delegate;
	private final IndexSchemaContributionListener listener;

	NotifyingNestingContext(IndexSchemaNestingContext delegate, IndexSchemaContributionListener listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public <T> T nest(String relativeName, Function<String, T> nestedElementFactoryIfIncluded,
			Function<String, T> nestedElementFactoryIfExcluded) {
		return delegate.nest(
				relativeName,
				prefixedName -> {
					listener.onSchemaContributed();
					return nestedElementFactoryIfIncluded.apply( prefixedName );
				},
				nestedElementFactoryIfExcluded
		);
	}

	@Override
	public <T> T nest(String relativeName,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfIncluded,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfExcluded) {
		return delegate.nest(
				relativeName,
				(prefixedName, nestingContext) -> {
					listener.onSchemaContributed();
					return nestedElementFactoryIfIncluded.apply(
							prefixedName,
							new NotifyingNestingContext( nestingContext, listener )
					);
				},
				nestedElementFactoryIfExcluded
		);
	}
}
