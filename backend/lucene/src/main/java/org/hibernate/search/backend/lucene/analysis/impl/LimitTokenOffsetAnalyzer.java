/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.LimitTokenOffsetFilter;

public class LimitTokenOffsetAnalyzer extends AnalyzerWrapper {

	public static Analyzer analyzer(Analyzer delegate, Integer maxOffset) {
		if ( maxOffset != null ) {
			return new LimitTokenOffsetAnalyzer( delegate, maxOffset );
		}
		return delegate;
	}

	private final Analyzer delegate;
	private final int maxOffset;

	private LimitTokenOffsetAnalyzer(Analyzer delegate, int maxOffset) {
		super( delegate.getReuseStrategy() );
		this.delegate = delegate;
		this.maxOffset = maxOffset;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return delegate;
	}

	@Override
	protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
		return new TokenStreamComponents(
				components.getSource(),
				new LimitTokenOffsetFilter( components.getTokenStream(), maxOffset, false )
		);
	}
}
