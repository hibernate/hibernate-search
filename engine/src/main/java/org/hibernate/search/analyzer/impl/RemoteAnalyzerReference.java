/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * A reference to a {@code RemoteAnalyzer}.
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class RemoteAnalyzerReference implements AnalyzerReference {

	private RemoteAnalyzer analyzer;

	public RemoteAnalyzerReference(RemoteAnalyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public RemoteAnalyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( RemoteAnalyzerReference.class );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		return analyzerType.cast( this );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		if ( analyzer != null ) {
			sb.append( analyzer );
		}
		sb.append( ">" );
		return sb.toString();
	}
}
