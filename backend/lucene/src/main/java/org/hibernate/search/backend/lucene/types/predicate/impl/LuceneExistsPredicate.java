/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneExistsPredicate extends AbstractLuceneSingleFieldPredicate {

	private final Query query;

	private LuceneExistsPredicate(Builder builder) {
		super( builder );
		query = builder.buildQuery();
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	public static class Builder extends AbstractBuilder implements ExistsPredicateBuilder {
		private final LuceneFieldCodec<?> codec;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<?> field,
				LuceneFieldCodec<?> codec) {
			super( searchContext, field );
			this.codec = codec;
			// Score is always constant for this query
			constantScore();
		}

		@Override
		public SearchPredicate build() {
			return new LuceneExistsPredicate( this );
		}

		private Query buildQuery() {
			return codec.createExistsQuery( absoluteFieldPath );
		}
	}
}
