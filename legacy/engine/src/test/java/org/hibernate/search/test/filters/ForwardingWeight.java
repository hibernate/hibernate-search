/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filters;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * @author Yoann Rodiere
 */
class ForwardingWeight extends Weight {

	private final Weight delegate;

	public ForwardingWeight(Query query, Weight delegate) {
		super( query );
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "(" ).append( delegate ).append( ")" )
				.toString();
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		delegate.extractTerms( terms );
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		return delegate.explain( context, doc );
	}

	@Override
	public float getValueForNormalization() throws IOException {
		return delegate.getValueForNormalization();
	}

	@Override
	public void normalize(float norm, float boost) {
		delegate.normalize( norm, boost );
	}

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		return delegate.scorer( context );
	}

	@Override
	public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
		return delegate.bulkScorer( context );
	}

}
