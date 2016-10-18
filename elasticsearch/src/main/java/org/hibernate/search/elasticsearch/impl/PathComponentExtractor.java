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
	 * Append to the path the part of {@code otherPath} that is relative to the current path.
	 * <p>In other words, replace the current path with {@code otherPath} provided {@code otherPath}
	 * denotes a child element of the current path, while preserving the memory of the previously
	 * consumed path components.
	 * @param otherPath A path that must start with the current path.
	 */
	public void appendRelativePart(String otherPath) {
		String pathAsString = path.toString();
		if ( !otherPath.startsWith( pathAsString ) ) {
			throw new AssertionFailure( "The path '" + otherPath + "' is not contained within '" + pathAsString + "'" );
		}

		path.append( otherPath, path.length(), otherPath.length() );
	}

	public enum ConsumptionLimit {
		SECOND_BUT_LAST,
		LAST;
	}

	/**
	 * Consumes one more component in the current path (if possible) and returns this component.
	 *
	 *<p>If this method reaches a incompletely qualified path component, i.e one that is not
	 *followed by a dot but by the end of the path, it will return it only if {@code includeLast}
	 *is {@code true}.
	 *
	 * @param includeLast Whether or not to return the last, incompletely qualified path component.
	 * @return The next path component.
	 * @throws AssertionFailure If there is nothing to consume in the path, or if there is more
	 * than one component to consume.
	 */
	public String next(ConsumptionLimit consumeLimit) {
		int nextSeparatorIndex = path.indexOf( PATH_COMPONENT_SEPARATOR, currentIndexInPath );
		if ( nextSeparatorIndex >= 0 ) {
			String childName = path.substring( currentIndexInPath, nextSeparatorIndex );
			currentIndexInPath = nextSeparatorIndex + 1 /* skip the dot */;
			return childName;
		}
		else if ( ConsumptionLimit.LAST.equals( consumeLimit ) && currentIndexInPath < path.length() ) {
			String lastComponent = path.substring( currentIndexInPath );
			currentIndexInPath = path.length();
			return lastComponent;
		}
		else {
			return null;
		}
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