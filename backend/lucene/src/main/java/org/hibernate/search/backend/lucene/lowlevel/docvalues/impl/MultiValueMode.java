/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.util.Locale;

/**
 * Defines what values to pick in the case a document contains multiple values
 * for a particular field.
 */
public enum MultiValueMode {

	/**
	 * Pick the sum of all the values.
	 */
	SUM, AVG, MIN, MAX, MEDIAN;

	/**
	 * A case insensitive version of {@link #valueOf(String)}
	 *
	 * @param sortMode
	 * @return
	 * @throws IllegalArgumentException if the given string doesn't match a
	 * sort mode or is <code>null</code>.
	 */
	public static MultiValueMode fromString(String sortMode) {
		try {
			return valueOf( sortMode.toUpperCase( Locale.ROOT ) );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Illegal sort mode: " + sortMode );
		}
	}

}
