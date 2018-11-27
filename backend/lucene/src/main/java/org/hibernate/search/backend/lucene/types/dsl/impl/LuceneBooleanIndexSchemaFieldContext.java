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
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneBooleanFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneIntegerFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneIntegerFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

public class LuceneBooleanIndexSchemaFieldContext
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneBooleanIndexSchemaFieldContext, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneBooleanIndexSchemaFieldContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Boolean.class );
	}

	@Override
	public LuceneBooleanIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Boolean> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		LuceneBooleanFieldConverter converter = new LuceneBooleanFieldConverter( helper.createUserIndexFieldConverter() );
		LuceneBooleanFieldCodec codec = new LuceneBooleanFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Boolean> schemaNode = new LuceneIndexSchemaFieldNode<>(
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
	protected LuceneBooleanIndexSchemaFieldContext thisAsS() {
		return this;
	}
}
