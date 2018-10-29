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
import org.hibernate.search.backend.lucene.types.codec.impl.IntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.IntegerFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.StandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.IntegerFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @author Guillaume Smet
 */
public class LuceneIntegerIndexSchemaFieldContextImpl
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneIntegerIndexSchemaFieldContextImpl, Integer> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneIntegerIndexSchemaFieldContextImpl(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, Integer.class );
	}

	@Override
	public LuceneIntegerIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<Integer> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		StandardFieldConverter<Integer> converter = new StandardFieldConverter<>( helper.createUserIndexFieldConverter() );
		IntegerFieldCodec codec = new IntegerFieldCodec( projectable, sortable );

		LuceneIndexSchemaFieldNode<Integer> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				new IntegerFieldPredicateBuilderFactory( converter ),
				new IntegerFieldSortBuilderFactory( sortable, converter ),
				new StandardFieldProjectionBuilderFactory<>( projectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneIntegerIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
