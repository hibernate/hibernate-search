/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

/**
 * Represents an arbitrarily long access path when walking the POJO model.
 * <p>
 * For instance the path could be:
 * <code>
 * property "propertyOfA" =&gt; extractor "MapValueExtractor" =&gt; property "propertyOfB"
 * </code>
 * Meaning: extract property "propertyOfA", then extract values using "MapValueExtractor",
 * then for each value extract property "propertyOfB".
 */
public abstract class PojoModelPath {

	public static PojoModelPathPropertyNode fromRoot(String propertyName) {
		return new PojoModelPathPropertyNode( null, propertyName );
	}

	PojoModelPath() {
		// Package-protected constructor
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( getClass().getSimpleName() )
				.append( "[" );
		appendPath( builder );
		builder.append( "]" );
		return builder.toString();
	}

	public abstract PojoModelPath parent();

	abstract void appendSelfPath(StringBuilder builder);

	private void appendPath(StringBuilder builder) {
		PojoModelPath parent = parent();
		if ( parent == null ) {
			appendSelfPath( builder );
		}
		else {
			parent.appendPath( builder );
			appendSelfPath( builder );
		}
	}
}
