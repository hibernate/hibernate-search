/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexSchemaFieldNode<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String relativeFieldName;

	private final LuceneIndexSchemaObjectNode parent;

	private final String absoluteFieldPath;

	private final boolean multiValued;

	private final LuceneFieldCodec<F> codec;

	private final LuceneFieldPredicateBuilderFactory predicateBuilderFactory;

	private final LuceneFieldSortBuilderFactory sortBuilderFactory;

	private final LuceneFieldProjectionBuilderFactory projectionBuilderFactory;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued,
			LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( relativeFieldName );
		this.multiValued = multiValued;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsoluteFieldPath() {
		return absoluteFieldPath;
	}

	/**
	 * @return {@code true} if this node is multi-valued in its parent object.
	 */
	public boolean isMultiValued() {
		return multiValued;
	}

	public LuceneFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		if ( predicateBuilderFactory == null ) {
			throw log.unsupportedDSLPredicates( getEventContext() );
		}
		return predicateBuilderFactory;
	}

	public LuceneFieldSortBuilderFactory getSortBuilderFactory() {
		if ( sortBuilderFactory == null ) {
			throw log.unsupportedDSLSorts( getEventContext() );
		}
		return sortBuilderFactory;
	}

	public LuceneFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		if ( projectionBuilderFactory == null ) {
			throw log.unsupportedDSLProjections( getEventContext() );
		}
		return projectionBuilderFactory;
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
