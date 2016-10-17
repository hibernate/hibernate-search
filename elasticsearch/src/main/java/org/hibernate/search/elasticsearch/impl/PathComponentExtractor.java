/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.exception.AssertionFailure;

/**
 * A stateful helper to extract path components from a string, one
 * component at a time.
 *
 * @author Yoann Rodiere
 */
class PathComponentExtractor implements Cloneable {

	private static final String PATH_COMPONENT_SEPARATOR = ".";

	/*
	 * Non-final for cloning only.
	 */
	private StringBuilder path = new StringBuilder();
	private int currentIndexInPath = 0;

	/**
	 * Append a string to the path for later consumption through {@link #next()}.
	 * @param pathPart A string that may or may not include path component
	 * separators (dots).
	 */
	public void append(String pathPart) {
		path.append( pathPart );
	}

	/**
	 * @return The next complete path component, or null if it cannot be determined yet.
	 */
	public String next() {
		int nextSeparatorIndex = path.indexOf( PATH_COMPONENT_SEPARATOR, currentIndexInPath );
		if ( nextSeparatorIndex >= 0 ) {
			String childName = path.substring( currentIndexInPath, nextSeparatorIndex );
			currentIndexInPath = nextSeparatorIndex + 1 /* skip the dot */;
			return childName;
		}
		else {
			return null;
		}
	}

	/**
	 * @param otherPath A path to make relative.
	 * @return The relative path from the currently consumed path (the components returned by {@link #next()}) to {@code otherPath}.
	 */
	public String makeRelative(String otherPath) {
		String pathAsString = path.toString();
		if ( !otherPath.startsWith( pathAsString ) ) {
			throw new AssertionFailure( "The path '" + otherPath + "' is not contained within '" + pathAsString + "'" );
		}

		return otherPath.substring( currentIndexInPath );
	}

	public void reset() {
		path.delete( 0, path.length() );
		currentIndexInPath = 0;
	}

	@Override
	public PathComponentExtractor clone() {
		try {
			PathComponentExtractor clone = (PathComponentExtractor) super.clone();
			clone.path = new StringBuilder( path );
			return clone;
		}
		catch (CloneNotSupportedException e) {
			throw new AssertionFailure( "Unexpected clone() failure", e );
		}
	}

	@Override
	public String toString() {
		return new StringBuilder( path ).insert( currentIndexInPath, "[]" ).toString();
	}

}