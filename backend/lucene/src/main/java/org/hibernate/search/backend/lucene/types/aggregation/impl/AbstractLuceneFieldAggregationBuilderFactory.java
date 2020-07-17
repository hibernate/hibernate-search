/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldAggregationBuilderFactory<F>
		implements LuceneFieldAggregationBuilderFactory<F> {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean aggregable;

	public AbstractLuceneFieldAggregationBuilderFactory(boolean aggregable) {
		this.aggregable = aggregable;
	}

	@Override
	public boolean isAggregable() {
		return aggregable;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldAggregationBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && getCodec().isCompatibleWith( castedOther.getCodec() );
	}

	protected abstract LuceneFieldCodec<F> getCodec();

	protected void checkAggregable(LuceneSearchFieldContext<?> field) {
		if ( !aggregable ) {
			throw log.nonAggregableField( field.absolutePath(), field.eventContext() );
		}
	}

}
