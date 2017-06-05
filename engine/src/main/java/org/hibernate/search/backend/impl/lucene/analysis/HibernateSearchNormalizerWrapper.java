/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

/**
 * @author Yoann Rodiere
 */
public class HibernateSearchNormalizerWrapper extends AnalyzerWrapper {

	private final Analyzer delegate;

	private final String normalizerName;

	public HibernateSearchNormalizerWrapper(Analyzer delegate, String normalizerName) {
		super( delegate.getReuseStrategy() );
		this.delegate = delegate;
		this.normalizerName = normalizerName;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return delegate;
	}

	@Override
	protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
		HibernateSearchNormalizerCheckingFilter filter =
				new HibernateSearchNormalizerCheckingFilter( components.getTokenStream(), normalizerName );
		return new TokenStreamComponents( components.getTokenizer(), filter );
	}

	@Override
	public String toString() {
		return "HibernateSearchNormalizerWrapper(" + delegate.toString() + ", normalizerName=" + normalizerName + ")";
	}

}
