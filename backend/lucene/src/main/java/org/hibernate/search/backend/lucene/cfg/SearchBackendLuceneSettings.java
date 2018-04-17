/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

/**
 * Configuration properties for Lucene,
 */
public final class SearchBackendLuceneSettings {

	private SearchBackendLuceneSettings() {
	}

	public static final String LUCENE_DIRECTORY_PROVIDER = "lucene.directory_provider";

	public static final String LUCENE_ROOT_DIRECTORY = "lucene.root_directory";

	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy_strategy";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final MultiTenancyStrategyConfiguration MULTI_TENANCY_STRATEGY = MultiTenancyStrategyConfiguration.NONE;
	}
}
