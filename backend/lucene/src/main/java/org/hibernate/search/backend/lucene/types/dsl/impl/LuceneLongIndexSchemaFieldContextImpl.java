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
import org.hibernate.search.backend.lucene.types.codec.impl.LongFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneLongFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.StandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LongFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

public class LuceneLongIndexSchemaFieldContextImpl
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneLongIndexSchemaFieldContextImpl, Long> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneLongIndexSchemaFieldContextImpl(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Long.class );
	}

	@Override
	public LuceneLongIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Long> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		StandardFieldConverter<Long> converter = new StandardFieldConverter<>( helper.createUserIndexFieldConverter() );
		LongFieldCodec codec = new LongFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<Long> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				new LuceneLongFieldPredicateBuilderFactory( converter ),
				new LongFieldSortBuilderFactory( resolvedSortable, converter ),
				new StandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneLongIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
