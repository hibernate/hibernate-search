/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;


/**
 * A copy of Lucene's ConstantScoreWeight implementation,
 * necessary because the one in Lucene is marked as "internal".
 *
 * @author Yoann Rodiere
 */
abstract class ConstantScoreWeight extends Weight {

	private float boost;
	private float queryNorm;
	private float queryWeight;

	protected ConstantScoreWeight(Query query) {
		super( query );
		normalize( 1f, 1f );
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		// most constant-score queries don't wrap index terms
		// eg. geo filters, doc values queries, ...
		// override if your constant-score query does wrap terms
	}

	@Override
	public final float getValueForNormalization() throws IOException {
		return queryWeight * queryWeight;
	}

	@Override
	public void normalize(float norm, float boost) {
		this.boost = boost;
		queryNorm = norm;
		queryWeight = queryNorm * boost;
	}

	protected final float score() {
		return queryWeight;
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		final Scorer s = scorer( context );
		final boolean exists;
		if ( s == null ) {
			exists = false;
		}
		else {
			final TwoPhaseIterator twoPhase = s.twoPhaseIterator();
			if ( twoPhase == null ) {
				exists = s.iterator().advance( doc ) == doc;
			}
			else {
				exists = twoPhase.approximation().advance( doc ) == doc && twoPhase.matches();
			}
		}

		if ( exists ) {
			return Explanation.match(
					queryWeight, getQuery().toString() + ", product of:",
					Explanation.match( boost, "boost" ), Explanation.match( queryNorm, "queryNorm" ) );
		}
		else {
			return Explanation.noMatch( getQuery().toString() + " doesn't match id " + doc );
		}
	}
}
