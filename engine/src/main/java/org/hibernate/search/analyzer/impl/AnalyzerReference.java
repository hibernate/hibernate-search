/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;

/**
 * Reference an analayzer implementation.
 * <p>
 * It can be a reference to an {@link Analyzer} or a {@link RemoteAnalyzer}.
 *
 * @author Davide D'Alto
 */
public class AnalyzerReference {

	private String name;
	private RemoteAnalyzer remote;
	private Analyzer analyzer;

	/**
	 * @return true if at least one referenced analyzer is not null
	 */
	public boolean isInitialized() {
		return remote != null || analyzer != null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public RemoteAnalyzer getRemote() {
		return remote;
	}

	public void setRemote(RemoteAnalyzer remote) {
		this.remote = remote;
	}

	public void close() {
		close( analyzer );
	}

	private void close(Analyzer analyzer) {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( name != null ) {
			builder.append( name );
			builder.append( " : " );
		}
		if ( remote != null ) {
			builder.append( remote );
			builder.append( ", " );
		}
		if ( analyzer != null ) {
			builder.append( analyzer );
		}
		return builder.toString();
	}
}
