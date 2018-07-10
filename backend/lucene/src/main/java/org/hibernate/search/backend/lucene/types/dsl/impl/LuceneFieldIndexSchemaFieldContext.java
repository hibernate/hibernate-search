/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.apache.lucene.index.IndexableField;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldFieldCodec;
import org.hibernate.search.backend.lucene.types.formatter.impl.SimpleCastingFieldFormatter;

/**
 * @author Guillaume Smet
 */
public class LuceneFieldIndexSchemaFieldContext<V, F extends IndexableField> extends AbstractLuceneIndexSchemaFieldTypedContext<V> {

	private LuceneFieldContributor<V> fieldContributor;

	private LuceneFieldValueExtractor<V> fieldValueExtractor;

	public LuceneFieldIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName,
			LuceneFieldContributor<V> fieldContributor, LuceneFieldValueExtractor<V> fieldValueExtractor) {
		super( schemaContext, relativeFieldName );

		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<V> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaFieldNode<V> schemaNode = new LuceneIndexSchemaFieldNode<V>(
				parentNode,
				getRelativeFieldName(),
				new SimpleCastingFieldFormatter<>(),
				new LuceneFieldFieldCodec<>( fieldContributor, fieldValueExtractor ),
				null,
				null
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
