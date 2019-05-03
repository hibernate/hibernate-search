/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query.impl;

import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultContext;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;

public class LuceneSearchQueryContextImpl<Q> extends AbstractDelegatingSearchQueryContext<
						LuceneSearchQueryContext<Q>,
						Q
						>
		implements LuceneSearchQueryResultContext<Q>, LuceneSearchQueryContext<Q> {

	// FIXME use the builder to make toQuery return a Lucene-specific query type
	private final LuceneSearchQueryBuilder<?> builder;

	public LuceneSearchQueryContextImpl(SearchQueryContextImplementor<?, Q> original,
			LuceneSearchQueryBuilder<?> builder) {
		super( original );
		this.builder = builder;
	}

	@Override
	protected LuceneSearchQueryContextImpl<Q> thisAsS() {
		return this;
	}

}
