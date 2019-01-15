/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

import org.apache.lucene.util.Version;

/**
 * Configuration properties for Lucene,
 */
public final class LuceneBackendSettings {

	private LuceneBackendSettings() {
	}

	/**
	 * The Lucene match version parameter. Highly recommended since Lucene 3.
	 */
	public static final String LUCENE_VERSION = "lucene_version";

	public static final String DIRECTORY_PROVIDER = "directory_provider";

	public static final String ROOT_DIRECTORY = "root_directory";

	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy_strategy";

	public static final String ANALYSIS_CONFIGURER = "analysis_configurer";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		/**
		 * If nothing else is specified we use {@code Version.LATEST} as the default Lucene version. This version
		 * parameter was introduced by Lucene to attempt providing backwards compatibility when upgrading Lucene versions
		 * and not wanting to rebuild the index from scratch. It's highly recommended to specify a version, so that you
		 * can upgrade Hibernate Search and control when to eventually upgrade the Lucene format.
		 */
		public static final Version LUCENE_VERSION = Version.LATEST;

		public static final MultiTenancyStrategyConfiguration MULTI_TENANCY_STRATEGY = MultiTenancyStrategyConfiguration.NONE;
	}
}
