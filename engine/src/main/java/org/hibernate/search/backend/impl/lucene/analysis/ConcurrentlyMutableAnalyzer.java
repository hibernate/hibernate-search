/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.analysis;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * An Analyzer implementation which can dynamically be reconfigured. We use this as temporary solution to workaround
 * LUCENE-6212, until a better solution is found.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public final class ConcurrentlyMutableAnalyzer extends DelegatingAnalyzerWrapper {

	private final AtomicReference<ScopedAnalyzer> current = new AtomicReference<>();

	public ConcurrentlyMutableAnalyzer(Analyzer initialAnalyzer) {
		super( new ResettableReuseStrategy() );
		if ( initialAnalyzer instanceof ScopedAnalyzer ) {
			current.set( (ScopedAnalyzer) initialAnalyzer );
		}
		else {
			current.set( new ScopedAnalyzer( initialAnalyzer ) );
		}
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return current.get();
	}

	/**
	 * Checks if the currently set Analyzer is the same as the
	 * proposed one, in which case there is no need for
	 * replacements or locking.
	 * Correct concurrency control requires external locking!
	 * @param analyzer the {@link ScopedAnalyzer} to use for comparison
	 * @return true if there is no need to replace the current Analyzer
	 */
	public boolean isCompatibleWith(ScopedAnalyzer analyzer) {
		ScopedAnalyzer currentAnalyzer = current.get();
		return currentAnalyzer.isCompositeOfSameInstances( analyzer );
	}

	/**
	 * Correct concurrency control requires external locking!
	 * @param analyzer the {@link ScopedAnalyzer} to use for locking
	 */
	public void updateAnalyzer(ScopedAnalyzer analyzer) {
		current.set( analyzer );
	}

	private static final class ResettableReuseStrategy extends ReuseStrategy {

		@Override
		public TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
			if ( analyzer instanceof ConcurrentlyMutableAnalyzer ) {
				ConcurrentlyMutableAnalyzer cma = (ConcurrentlyMutableAnalyzer) analyzer;
				final Analyzer wrappedAnalyzer = cma.getWrappedAnalyzer( fieldName );
				return wrappedAnalyzer.getReuseStrategy().getReusableComponents( wrappedAnalyzer, fieldName );
			}
			else {
				throw new AssertionFailure( "This ReuseStrategy should only be applied to a ConcurrentlyMutableAnalyzer " );
			}
		}

		@Override
		public void setReusableComponents(Analyzer analyzer, String fieldName, TokenStreamComponents components) {
			if ( analyzer instanceof ConcurrentlyMutableAnalyzer ) {
				ConcurrentlyMutableAnalyzer cma = (ConcurrentlyMutableAnalyzer) analyzer;
				final Analyzer wrappedAnalyzer = cma.getWrappedAnalyzer( fieldName );
				wrappedAnalyzer.getReuseStrategy().setReusableComponents( analyzer, fieldName, components );
			}
			else {
				throw new AssertionFailure( "This ReuseStrategy should only be applied to a ConcurrentlyMutableAnalyzer " );
			}
		}
	};

}
