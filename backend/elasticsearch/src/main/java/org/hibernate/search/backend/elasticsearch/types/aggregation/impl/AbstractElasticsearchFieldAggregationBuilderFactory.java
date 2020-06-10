/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchFieldAggregationBuilderFactory<F>
		implements ElasticsearchFieldAggregationBuilderFactory<F> {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean aggregable;

	protected final ElasticsearchFieldCodec<F> codec;

	public AbstractElasticsearchFieldAggregationBuilderFactory(boolean aggregable, ElasticsearchFieldCodec<F> codec) {
		this.aggregable = aggregable;
		this.codec = codec;
	}

	@Override
	public boolean isAggregable() {
		return aggregable;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldAggregationBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractElasticsearchFieldAggregationBuilderFactory<?> castedOther =
				(AbstractElasticsearchFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && codec.isCompatibleWith( castedOther.codec );
	}

	protected void checkAggregable(ElasticsearchSearchFieldContext<?> field) {
		if ( !aggregable ) {
			throw log.nonAggregableField( field.absolutePath(), field.eventContext() );
		}
	}
}
