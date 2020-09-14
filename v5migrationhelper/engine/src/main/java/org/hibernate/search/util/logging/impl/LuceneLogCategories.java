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
public final class LuceneLogCategories {

	private LuceneLogCategories() {
	}

	/**
	 * This is the category of the Logger used to print out the Lucene infostream.
	 * To enable the logger, the category needs to be enabled at TRACE level and configuration
	 * property {@code org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM}
	 * needs to be enabled on the index.
	 *
	 * @see org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM
	 */
	public static final LogCategory INFOSTREAM_LOGGER_CATEGORY = new LogCategory( "org.hibernate.search.backend.lucene.infostream" );

}
