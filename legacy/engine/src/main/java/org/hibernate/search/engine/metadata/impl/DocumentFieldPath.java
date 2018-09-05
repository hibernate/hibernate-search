/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.search.exception.AssertionFailure;

/**
 * @author Yoann Rodiere
 */
public final class DocumentFieldPath {

	private final String prefix;
	private final String relativeName;
	private final String absoluteName;

	public DocumentFieldPath(String prefix, String relativeName) {
		super();
		if ( prefix == null ) {
			throw new AssertionFailure( "prefix must not be null" );
		}
		this.prefix = prefix;
		if ( relativeName == null ) {
			throw new AssertionFailure( "relativeName must not be null" );
		}
		this.relativeName = relativeName;

		/*
		 * The absolute name is most likely to be accessed, and repeatedly so.
		 * Thus we cache its value right from the start.
		 */
		this.absoluteName = prefix + relativeName;
	}

	/**
	 * @return The concatenated indexed-embedded prefixes, or an empty string if there isn't any.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @return The field name excluding any indexed-embedded prefix.
	 */
	public String getRelativeName() {
		return relativeName;
	}

	/**
	 * @return The full field name, including any indexed-embedded prefix.
	 */
	public String getAbsoluteName() {
		return absoluteName;
	}

	/**
	 * @return {@code true} if {@code obj} is a {@link DocumentFieldPath} with the exact same
	 * absolute name, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if ( obj != null && obj.getClass().equals( getClass() ) ) {
			DocumentFieldPath other = (DocumentFieldPath) obj;
			return absoluteName.equals( other.absoluteName );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getAbsoluteName().hashCode();
	}

	@Override
	public String toString() {
		return getAbsoluteName();
	}
}
