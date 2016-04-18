/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

/**
 * An analyzer defined on the backend.
 *
 * @author Davide D'Alto
 */
public final class RemoteAnalyzerReference implements AnalyzerReference {

	private final String name;

	public RemoteAnalyzerReference(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void close() {
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return RemoteAnalyzerReference.class.isAssignableFrom( analyzerType );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		return (T) this;
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
