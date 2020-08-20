/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.util.ToStringUtils;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;

/**
 * A phrase query interpreted remotely, thus based on the remote analyzers.
 *
 * @author Guillaume Smet
 */
public class RemotePhraseQuery extends AbstractRemoteQueryWithAnalyzer {

	private String field;

	private String phrase;

	private int slop;

	public RemotePhraseQuery(String field, int slop, String phrase,
			RemoteAnalyzerReference originalAnalyzerReference, RemoteAnalyzerReference queryAnalyzerReference) {
		super( originalAnalyzerReference, queryAnalyzerReference );
		this.field = field;
		this.slop = slop;
		this.phrase = phrase;
	}

	public String getField() {
		return field;
	}

	public String getPhrase() {
		return phrase;
	}

	public int getSlop() {
		return slop;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() ).append( "<" );
		sb.append( field ).append( ":" );
		sb.append( "\"" ).append( phrase ).append( "\"" );
		if ( slop != 0 ) {
			sb.append( "~" ).append( slop );
		}
		sb.append( ToStringUtils.boost( getBoost() ) );
		sb.append( ", originalAnalyzer=" ).append( getOriginalAnalyzerReference() );
		sb.append( ", queryAnalyzer=" ).append( getQueryAnalyzerReference() );
		sb.append( ">" );
		return sb.toString();
	}

}
