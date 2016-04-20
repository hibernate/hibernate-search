/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A remote {@code Analyzer} loaded lazily.
 *
 * @author Guillaume Smet
 */
public class LazyRemoteAnalyzer extends RemoteAnalyzer {

	private static final Log log = LoggerFactory.make();

	private RemoteAnalyzer delegate;

	public LazyRemoteAnalyzer(String name) {
		super( name );
	}

	public String getName() {
		return name;
	}

	@Override
	public String getName(String fieldName) {
		validate();
		return name;
	}

	@Override
	public void close() {
		if ( delegate != null ) {
			delegate.close();
		}
	}

	public void setDelegate(RemoteAnalyzer analyzer) {
		this.delegate = analyzer;
	}

	private void validate() {
		if ( delegate == null ) {
			throw log.lazyRemoteAnalyzerNotInitialized( this );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( name );
		if ( delegate != null ) {
			sb.append( ", delegate: " );
			sb.append( delegate );
		}
		sb.append( ">" );
		return sb.toString();
	}

}
