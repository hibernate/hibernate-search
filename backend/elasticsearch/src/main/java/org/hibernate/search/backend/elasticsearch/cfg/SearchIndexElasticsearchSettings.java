/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Configuration properties common to all Hibernate Search indexes regardless of the underlying technology.
 */
public final class SearchIndexElasticsearchSettings {

	private SearchIndexElasticsearchSettings() {
	}

	public static final String REFRESH_AFTER_WRITE = "refresh_after_write";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean REFRESH_AFTER_WRITE = false;
	}

}
