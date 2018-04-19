/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LocalDateFieldCodec;
import org.hibernate.search.backend.lucene.types.formatter.impl.LocalDateFieldFormatter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LocalDateFieldQueryFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LocalDateFieldSortContributor;

/**
 * @author Guillaume Smet
 */
public class LocalDateIndexSchemaFieldContext extends AbstractLuceneIndexSchemaFieldTypedContext<LocalDate> {

	private Sortable sortable;

	public LocalDateIndexSchemaFieldContext(String fieldName) {
		super( fieldName );
	}

	@Override
	public LocalDateIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<LocalDate> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaFieldNode<LocalDate> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getFieldName(),
				LocalDateFieldFormatter.INSTANCE,
				new LocalDateFieldCodec( getStore(), sortable ),
				new LocalDateFieldQueryFactory( LocalDateFieldFormatter.INSTANCE ),
				LocalDateFieldSortContributor.INSTANCE
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
