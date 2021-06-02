/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class LuceneExistsPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneExistsPredicate(AbstractBuilder<?> builder) {
		super( builder );
	}

	private abstract static class AbstractBuilder<F> extends AbstractLuceneLeafSingleFieldPredicate.AbstractBuilder<F>
			implements ExistsPredicateBuilder {
		private AbstractBuilder(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			super( scope, field );
			// Score is always constant for this query
			constantScore();
		}

		@Override
		public SearchPredicate build() {
			return new LuceneExistsPredicate( this );
		}

		@Override
		protected abstract Query buildQuery();
	}

	public static class NormsBasedFactory
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, String> {
		@Override
		public NormsBasedBuilder create(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<String> field) {
			return new NormsBasedBuilder( scope, field );
		}
	}

	private static class NormsBasedBuilder extends AbstractBuilder<String> implements ExistsPredicateBuilder {
		private NormsBasedBuilder(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		protected Query buildQuery() {
			return new NormsFieldExistsQuery( absoluteFieldPath );
		}
	}

	public static class DocValuesBasedFactory<F>
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public DocValuesBasedBuilder<F> create(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			return new DocValuesBasedBuilder<>( scope, field );
		}
	}

	private static class DocValuesBasedBuilder<F> extends AbstractBuilder<F> implements ExistsPredicateBuilder {
		private DocValuesBasedBuilder(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			super( scope, field );
		}

		@Override
		protected Query buildQuery() {
			return new DocValuesFieldExistsQuery( absoluteFieldPath );
		}
	}

	public static class DefaultFactory<F>
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public DefaultBuilder<F> create(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			return new DefaultBuilder<>( scope, field );
		}
	}

	private static class DefaultBuilder<F> extends AbstractBuilder<F> implements ExistsPredicateBuilder {
		private DefaultBuilder(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			super( scope, field );
		}

		@Override
		protected Query buildQuery() {
			return new TermQuery( new Term( MetadataFields.fieldNamesFieldName(), absoluteFieldPath ) );
		}
	}

}
