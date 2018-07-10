/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.IntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.formatter.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.types.formatter.impl.SimpleCastingFieldFormatter;
import org.hibernate.search.backend.lucene.types.predicate.impl.IntegerFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.IntegerFieldSortContributor;

/**
 * @author Guillaume Smet
 */
public class IntegerIndexSchemaFieldContext extends AbstractLuceneIndexSchemaFieldTypedContext<Integer> {

	public static final LuceneFieldFormatter<Integer> FORMATTER = new SimpleCastingFieldFormatter<>();

	private Sortable sortable;

	public IntegerIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName );
	}

	@Override
	public IntegerIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<Integer> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaFieldNode<Integer> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				FORMATTER,
				new IntegerFieldCodec( getStore(), sortable ),
				IntegerFieldPredicateBuilderFactory.INSTANCE,
				IntegerFieldSortContributor.INSTANCE
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
