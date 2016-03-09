/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.exception.SearchException;

/**
 * @author Davide D'Alto
 */
public final class LazyAnalyzerReference implements AnalyzerReference {

	private final String name;
	private AnalyzerReference delegate;

	public LazyAnalyzerReference(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void close() {
	}

	public void setDelegate(AnalyzerReference reference) {
		this.delegate = reference;
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		validate();
		if ( LazyAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			return true;
		}
		return delegate.is( analyzerType );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		if ( LazyAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			return (T) this;
		}
		validate();
		return delegate.unwrap( analyzerType );
	}

	private void validate() {
		if ( delegate == null ) {
			throw new SearchException( "Analyzer not initialized" );
		}
	}
}
