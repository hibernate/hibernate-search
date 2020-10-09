/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
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

	private static class AbstractBuilder<F> extends AbstractLuceneLeafSingleFieldPredicate.AbstractBuilder<F>
			implements ExistsPredicateBuilder {
		private AbstractBuilder(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			super( searchContext, field );
			// Score is always constant for this query
			constantScore();
		}

		@Override
		public SearchPredicate build() {
			return new LuceneExistsPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return new NormsFieldExistsQuery( absoluteFieldPath );
		}
	}

	public static class NormsBasedFactory
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, String> {
		@Override
		public NormsBasedBuilder create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<String> field) {
			return new NormsBasedBuilder( searchContext, field );
		}
	}

	private static class NormsBasedBuilder extends AbstractBuilder<String> implements ExistsPredicateBuilder {
		private NormsBasedBuilder(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<String> field) {
			super( searchContext, field );
		}

		@Override
		protected Query buildQuery() {
			return new NormsFieldExistsQuery( absoluteFieldPath );
		}
	}

	public static class DocValuesBasedFactory<F>
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public DocValuesBasedBuilder<F> create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			return new DocValuesBasedBuilder<>( searchContext, field );
		}
	}

	private static class DocValuesBasedBuilder<F> extends AbstractBuilder<F> implements ExistsPredicateBuilder {
		private DocValuesBasedBuilder(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			super( searchContext, field );
		}

		@Override
		protected Query buildQuery() {
			return new DocValuesFieldExistsQuery( absoluteFieldPath );
		}
	}

	public static class DefaultFactory<F>
			extends AbstractLuceneSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public DefaultBuilder<F> create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			return new DefaultBuilder<>( searchContext, field );
		}
	}

	private static class DefaultBuilder<F> extends AbstractBuilder<F> implements ExistsPredicateBuilder {
		private DefaultBuilder(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			super( searchContext, field );
		}

		@Override
		protected Query buildQuery() {
			return new TermQuery( new Term( MetadataFields.fieldNamesFieldName(), absoluteFieldPath ) );
		}
	}

}
