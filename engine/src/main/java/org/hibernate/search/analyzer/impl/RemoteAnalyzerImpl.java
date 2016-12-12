/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;


/**
 * An analyzer which is remote ie managed by the backend.
 *
 * @author Guillaume Smet
 */
public class RemoteAnalyzerImpl implements RemoteAnalyzer {

	protected String name;

	public RemoteAnalyzerImpl(String name) {
		this.name = name;
	}

	@Override
	public String getName(String fieldName) {
		return name;
	}

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( name );
		sb.append( ">" );
		return sb.toString();
	}

}
