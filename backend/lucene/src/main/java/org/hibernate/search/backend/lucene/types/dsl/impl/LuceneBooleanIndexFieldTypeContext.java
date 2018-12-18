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
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

public class LuceneBooleanIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneBooleanIndexFieldTypeContext, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneBooleanIndexFieldTypeContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Boolean.class );
	}

	@Override
	public LuceneBooleanIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Boolean> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends Boolean> dslToIndexConverter =
				helper.createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Boolean, ?> indexToProjectionConverter =
				helper.createIndexToProjectionConverter();
		LuceneBooleanFieldCodec codec = new LuceneBooleanFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Boolean> schemaNode = new LuceneIndexSchemaFieldNode<>(
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
	protected LuceneBooleanIndexFieldTypeContext thisAsS() {
		return this;
	}
}
