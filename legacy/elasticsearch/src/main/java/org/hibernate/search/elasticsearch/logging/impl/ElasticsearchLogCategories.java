/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import org.hibernate.search.util.logging.impl.LogCategory;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Log categories to be used with {@link LoggerFactory#make(LogCategory)}.
 *
 * @author Yoann Rodiere
 */
public final class ElasticsearchLogCategories {

	private ElasticsearchLogCategories() {
	}

	/**
	 * This is the category of the Logger used to print out executed Elasticsearch requests,
	 * along with the execution time.
	 * <p>
	 * To enable the logger, the category needs to be enabled at TRACE level.
	 */
	public static final LogCategory REQUEST = new LogCategory( "org.hibernate.search.elasticsearch.request" );

}
