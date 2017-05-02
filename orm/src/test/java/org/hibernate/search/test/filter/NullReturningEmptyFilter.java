/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * Apparently it's legal for Lucene weight to return a null scorer
 * on {@link Weight#scorer(LeafReaderContext)} : make sure we can deal with it as well.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class NullReturningEmptyFilter extends Query implements Serializable {

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		return new NullReturningEmptyWeight( this );
	}

	@Override
	public String toString(String field) {
		return "";
	}

	public class NullReturningEmptyWeight extends Weight {

		protected NullReturningEmptyWeight(Query query) {
			super( query );
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			// No-op
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			return Explanation.noMatch( "Empty filter" );
		}

		@Override
		public float getValueForNormalization() throws IOException {
			return 0;
		}

		@Override
		public void normalize(float norm, float boost) {
			// No-op
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			return null;
		}

	}
}
