/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchFieldSortBuilderFactory<F>
		implements ElasticsearchFieldSortBuilderFactory<F> {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean sortable;
	protected final ElasticsearchFieldCodec<F> codec;

	public AbstractElasticsearchFieldSortBuilderFactory(boolean sortable, ElasticsearchFieldCodec<F> codec) {
		this.sortable = sortable;
		this.codec = codec;
	}

	@Override
	public boolean isSortable() {
		return sortable;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldSortBuilderFactory<?> other) {
		if ( this == other ) {
			return true;
		}
		if ( other.getClass() != getClass() ) {
			return false;
		}

		AbstractElasticsearchFieldSortBuilderFactory<?> castedOther = (AbstractElasticsearchFieldSortBuilderFactory<?>) other;
		return sortable == castedOther.sortable && codec.isCompatibleWith( castedOther.codec );
	}

	protected void checkSortable(ElasticsearchSearchFieldContext<?> field) {
		if ( !sortable ) {
			throw log.unsortableField( field.absolutePath(), field.eventContext() );
		}
	}
}
