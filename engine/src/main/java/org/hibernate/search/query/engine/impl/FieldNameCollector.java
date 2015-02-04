/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Helper class to extract field names from the Lucene queries taking query types into account.
 *
 * @author Hardy Ferentschik
 */
public class FieldNameCollector {

	private FieldNameCollector() {
	}

	public static FieldCollection extractFieldNames(Query query) {
		// first we need to find all composing queries since some query types are just containers
		Set<Query> composingQueries = new HashSet<>();
		collectComposingQueries( query, composingQueries );

		FieldCollection fieldCollection = new FieldCollection();
		for ( Query composingQuery : composingQueries ) {
			if ( composingQuery instanceof NumericRangeQuery ) {
				fieldCollection.addNumericFieldName( ( (NumericRangeQuery) composingQuery ).getField() );
			}
			else if ( composingQuery instanceof MultiTermQuery ) {
				fieldCollection.addStringFieldName( ( (MultiTermQuery) composingQuery ).getField() );
			}
			else if ( composingQuery instanceof TermQuery ) {
				TermQuery termQuery = (TermQuery) composingQuery;
				fieldCollection.addStringFieldName( termQuery.getTerm().field() );

			}
			else if ( composingQuery instanceof PhraseQuery ) {
				PhraseQuery phraseQuery = (PhraseQuery) composingQuery;
				// all terms must be against the same field, it's enough to look at the first
				fieldCollection.addStringFieldName( phraseQuery.getTerms()[0].field() );
			}
			else if ( composingQuery instanceof MultiPhraseQuery ) {
				MultiPhraseQuery phraseQuery = (MultiPhraseQuery) composingQuery;
				// all terms must be against the same field, it's enough to look at the first
				fieldCollection.addStringFieldName( phraseQuery.getTermArrays().get( 0 )[0].field() );
			}
		}

		return fieldCollection;
	}

	private static void collectComposingQueries(Query query, Set<Query> composingQueries) {
		if ( query instanceof BooleanQuery ) {
			BooleanQuery booleanQuery = (BooleanQuery) query;
			for ( BooleanClause clause : booleanQuery.getClauses() ) {
				collectComposingQueries( clause.getQuery(), composingQueries );
			}
		}
		else if ( query instanceof DisjunctionMaxQuery ) {
			DisjunctionMaxQuery disjunctionMaxQuery = (DisjunctionMaxQuery) query;
			for ( Query subQuery : disjunctionMaxQuery.getDisjuncts() ) {
				collectComposingQueries( subQuery, composingQueries );
			}
		}
		else if ( query instanceof ConstantScoreQuery ) {
			// this one is tricky, the ConstantScoreQuery can wrap a query or a filter.
			ConstantScoreQuery constantScoreQuery = (ConstantScoreQuery) query;
			if ( constantScoreQuery.getQuery() != null ) {
				collectComposingQueries( constantScoreQuery.getQuery(), composingQueries );
			}
		}
		else {
			composingQueries.add( query );
		}
	}

	public static class FieldCollection {
		private final Set<String> numericFieldNames;
		private final Set<String> stringFieldNames;

		public FieldCollection() {
			numericFieldNames = new HashSet<>();
			stringFieldNames = new HashSet<>();
		}

		void addNumericFieldName(String fieldName) {
			numericFieldNames.add( fieldName );
		}

		void addStringFieldName(String fieldName) {
			stringFieldNames.add( fieldName );
		}

		public Set<String> getNumericFieldNames() {
			return numericFieldNames;
		}

		public Set<String> getStringFieldNames() {
			return stringFieldNames;
		}

		@Override
		public String toString() {
			return "FieldCollection{" +
					"numericFieldNames=" + numericFieldNames +
					", stringFieldNames=" + stringFieldNames +
					'}';
		}
	}
}


