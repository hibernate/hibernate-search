/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

/**
 * Log categories to be used with {@link LoggerFactory#make(LogCategory)}.
 *
 * @author Gunnar Morling
 */
public final class DefaultLogCategories {

	private DefaultLogCategories() {
	}

	/**
	 * Category for logging executed search queries.
	 */
	public static final LogCategory QUERY = new LogCategory( "org.hibernate.search.fulltext_query" );

}
