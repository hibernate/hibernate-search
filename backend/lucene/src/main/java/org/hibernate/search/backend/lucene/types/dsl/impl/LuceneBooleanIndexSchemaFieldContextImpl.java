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
import org.hibernate.search.backend.lucene.types.codec.impl.BooleanFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.BooleanFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.IntegerFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.StandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.BooleanFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

public class LuceneBooleanIndexSchemaFieldContextImpl
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneBooleanIndexSchemaFieldContextImpl, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneBooleanIndexSchemaFieldContextImpl(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Boolean.class );
	}

	@Override
	public LuceneBooleanIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Boolean> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		BooleanFieldConverter converter = new BooleanFieldConverter( helper.createUserIndexFieldConverter() );
		BooleanFieldCodec codec = new BooleanFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Boolean> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				new IntegerFieldPredicateBuilderFactory( converter ),
				new BooleanFieldSortBuilderFactory( resolvedSortable, converter ),
				new StandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneBooleanIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
