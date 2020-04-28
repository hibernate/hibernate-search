/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexSchemaFieldNode<F> {

	private final String relativeFieldName;

	private final LuceneIndexSchemaObjectNode parent;

	private final String absoluteFieldPath;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final LuceneFieldCodec<F> codec;

	private final LuceneFieldPredicateBuilderFactory predicateBuilderFactory;

	private final LuceneFieldSortBuilderFactory sortBuilderFactory;

	private final LuceneFieldProjectionBuilderFactory projectionBuilderFactory;

	private final LuceneFieldAggregationBuilderFactory aggregationBuilderFactory;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued,
			LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( relativeFieldName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.multiValued = multiValued;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsoluteFieldPath() {
		return absoluteFieldPath;
	}

	public String getNestedDocumentPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	/**
	 * @return {@code true} if this node is multi-valued in its parent object.
	 */
	public boolean isMultiValued() {
		return multiValued;
	}

	public LuceneFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public LuceneFieldSortBuilderFactory getSortBuilderFactory() {
		return sortBuilderFactory;
	}

	public LuceneFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	public LuceneFieldAggregationBuilderFactory getAggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", relativeFieldName=" ).append( relativeFieldName )
				.append( ", codec=" ).append( codec )
				.append( ", predicateBuilderFactory=" ).append( predicateBuilderFactory )
				.append( ", sortContributor=" ).append( sortBuilderFactory )
				.append( ", projectionBuilderFactory=" ).append( projectionBuilderFactory )
				.append( "]" );
		return sb.toString();
	}

	private EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
	}

	public LuceneFieldCodec<F> getCodec() {
		return codec;
	}
}
