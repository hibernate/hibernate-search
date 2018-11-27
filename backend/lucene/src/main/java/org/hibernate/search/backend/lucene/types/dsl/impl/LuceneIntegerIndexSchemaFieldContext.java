/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneIntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneStandardFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneIntegerFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneIntegerFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @author Guillaume Smet
 */
public class LuceneIntegerIndexSchemaFieldContext
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneIntegerIndexSchemaFieldContext, Integer> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneIntegerIndexSchemaFieldContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Integer.class );
	}

	@Override
	public LuceneIntegerIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Integer> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		LuceneStandardFieldConverter<Integer> converter = new LuceneStandardFieldConverter<>( helper.createUserIndexFieldConverter() );
		LuceneIntegerFieldCodec codec = new LuceneIntegerFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Integer> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				new LuceneIntegerFieldPredicateBuilderFactory( converter ),
				new LuceneIntegerFieldSortBuilderFactory( resolvedSortable, converter ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneIntegerIndexSchemaFieldContext thisAsS() {
		return this;
	}
}
