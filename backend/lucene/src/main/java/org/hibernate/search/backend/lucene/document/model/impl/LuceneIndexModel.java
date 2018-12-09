/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.dsl.impl.ScopedAnalyzer;
import org.hibernate.search.engine.backend.document.converter.ToIndexIdValueConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.CollectionHelper;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexModel implements AutoCloseable {

	private static final ToIndexIdValueConverter<Object> NOOP_ID_CONVERTER = new ToIndexIdValueConverter<Object>() { };

	private final String indexName;

	private final Map<String, LuceneIndexSchemaObjectNode> objectNodes;

	private final Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes;

	private final ScopedAnalyzer scopedAnalyzer;

	private final ToIndexIdValueConverter<?> idConverter;

	public LuceneIndexModel(String indexName,
			ToIndexIdValueConverter<?> idConverter,
			Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder,
			Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder,
			ScopedAnalyzer scopedAnalyzer) {
		this.indexName = indexName;
		this.fieldNodes = CollectionHelper.toImmutableMap( fieldNodesBuilder );
		this.objectNodes = CollectionHelper.toImmutableMap( objectNodesBuilder );
		this.scopedAnalyzer = scopedAnalyzer;
		this.idConverter = idConverter == null ? NOOP_ID_CONVERTER : idConverter;
	}

	@Override
	public void close() {
		scopedAnalyzer.close();
	}

	public ToIndexIdValueConverter<?> getIdConverter() {
		return idConverter;
	}

	public String getIndexName() {
		return indexName;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}

	public LuceneIndexSchemaFieldNode<?> getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	public LuceneIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
	}

	public ScopedAnalyzer getScopedAnalyzer() {
		return scopedAnalyzer;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( "]" )
				.toString();
	}
}
