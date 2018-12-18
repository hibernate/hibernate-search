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
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @author Guillaume Smet
 */
public class LuceneIntegerIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneIntegerIndexFieldTypeContext, Integer> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneIntegerIndexFieldTypeContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Integer.class );
	}

	@Override
	public LuceneIntegerIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Integer> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends Integer> dslToIndexConverter =
				helper.createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Integer, ?> indexToProjectionConverter =
				helper.createIndexToProjectionConverter();
		LuceneIntegerFieldCodec codec = new LuceneIntegerFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Integer> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneIntegerIndexFieldTypeContext thisAsS() {
		return this;
	}
}
