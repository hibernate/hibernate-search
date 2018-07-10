/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldFieldCodec;
import org.hibernate.search.backend.lucene.types.formatter.impl.SimpleCastingFieldFormatter;

import org.apache.lucene.index.IndexableField;

/**
 * @author Guillaume Smet
 */
public class LuceneFieldIndexSchemaFieldContext<V, F extends IndexableField>
		implements IndexSchemaFieldTerminalContext<V>, LuceneIndexSchemaNodeContributor {

	private final IndexSchemaContext schemaContext;
	private final String relativeFieldName;
	private final LuceneFieldContributor<V> fieldContributor;
	private final LuceneFieldValueExtractor<V> fieldValueExtractor;

	private final DeferredInitializationIndexFieldAccessor<V> accessor = new DeferredInitializationIndexFieldAccessor<>();

	public LuceneFieldIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName,
			LuceneFieldContributor<V> fieldContributor, LuceneFieldValueExtractor<V> fieldValueExtractor) {
		this.schemaContext = schemaContext;
		this.relativeFieldName = relativeFieldName;
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public IndexFieldAccessor<V> createAccessor() {
		return accessor;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaFieldNode<V> schemaNode = new LuceneIndexSchemaFieldNode<V>(
				parentNode,
				relativeFieldName,
				new SimpleCastingFieldFormatter<>(),
				new LuceneFieldFieldCodec<>( fieldContributor, fieldValueExtractor ),
				null,
				null
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
