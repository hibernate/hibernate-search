/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;

/**
 * @author Yoann Rodiere
 */
public class ScopedRemoteAnalyzerReference extends RemoteAnalyzerReference implements ScopedAnalyzerReference {

	public ScopedRemoteAnalyzerReference(ScopedRemoteAnalyzer analyzer) {
		super( analyzer );
	}

	@Override
	public ScopedRemoteAnalyzer getAnalyzer() {
		return (ScopedRemoteAnalyzer) super.getAnalyzer();
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( ScopedRemoteAnalyzerReference.class );
	}

}
