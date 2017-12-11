/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollectorImplementor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

public class IndexModelBindingContextImpl implements IndexModelBindingContext {

	private final IndexModelCollectorImplementor collector;
	private final IndexModelNestingContextImpl nestingContext;

	private IndexModelCollector collectorWithContext;

	public IndexModelBindingContextImpl(IndexModelCollectorImplementor collector,
			IndexableTypeOrdering typeOrdering) {
		this( collector, new IndexModelNestingContextImpl( typeOrdering ) );
	}

	private IndexModelBindingContextImpl(IndexModelCollectorImplementor collector,
			IndexModelNestingContextImpl nestingContext) {
		this.collector = collector;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "collector=" ).append( collector )
				.append( ",nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexModelCollector getModelCollector() {
		if ( collectorWithContext == null ) {
			collectorWithContext = collector.withContext( nestingContext );
		}
		return collectorWithContext;
	}

	@Override
	public void explicitRouting() {
		collector.explicitRouting();
	}

	@Override
	public Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(IndexedTypeIdentifier parentTypeId,
			String relativePrefix, Integer nestedMaxDepth, Set<String> nestedPathFilters) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				relativePrefix,
				f -> f.composeWithNested( parentTypeId, relativePrefix, nestedMaxDepth, nestedPathFilters ),
				collector, IndexModelCollectorImplementor::childObject,
				IndexModelBindingContextImpl::new
		);
	}

}
