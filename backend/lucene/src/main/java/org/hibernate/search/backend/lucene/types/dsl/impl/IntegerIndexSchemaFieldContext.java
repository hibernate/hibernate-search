/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.formatter.impl.IntegerFieldFormatter;
import org.hibernate.search.backend.lucene.types.predicate.impl.IntegerFieldQueryFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.IntegerFieldSortContributor;

/**
 * @author Guillaume Smet
 */
public class IntegerIndexSchemaFieldContext extends AbstractLuceneIndexSchemaFieldTypedContext<Integer> {

	private Sortable sortable;

	public IntegerIndexSchemaFieldContext(String fieldName) {
		super( fieldName );
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
				getFieldName(),
				new IntegerFieldFormatter( getStore(), sortable ),
				IntegerFieldQueryFactory.INSTANCE,
				IntegerFieldSortContributor.INSTANCE
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
