/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.lowlevel.directory.FileSystemAccessStrategyName;
import org.hibernate.search.backend.lucene.multitenancy.MultiTenancyStrategyName;

import org.apache.lucene.util.Version;

/**
 * Configuration properties for Lucene backends.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 */
public final class LuceneBackendSettings {

	private LuceneBackendSettings() {
	}

	/**
	 * The name to use for the {@link org.hibernate.search.engine.cfg.BackendSettings#TYPE backend type}
	 * configuration property so that a Lucene backend is instantiated by Hibernate Search.
	 * <p>
	 * Only useful if you have more than one backend technology in the classpath;
	 * otherwise the backend type is automatically detected.
	 */
	public static final String TYPE_NAME = "lucene";

	/**
	 * The Lucene version to passed to analyzers when they are created.
	 * <p>
	 * This should be set in order to get consistent behavior when Lucene is upgraded.
	 * <p>
	 * Expects a {@link org.apache.lucene.util.Version},
	 * or a String accepted by {@link org.apache.lucene.util.Version#parseLeniently(java.lang.String)}
	 * <p>
	 * Defaults to {@link Defaults#LUCENE_VERSION}, which may change when Hibernate Search or Lucene is upgraded,
	 * and therefore is really not a good choice. You really should set this property with your own value.
	 */
	public static final String LUCENE_VERSION = "lucene_version";

	/**
	 * @deprecated Use {@link LuceneIndexSettings#DIRECTORY_PREFIX} instead.
	 */
	@Deprecated
	public static final String DIRECTORY_PREFIX = "directory.";

	/**
	 * @deprecated Use {@link LuceneIndexSettings#DIRECTORY_TYPE} instead.
	 */
	@Deprecated
	public static final String DIRECTORY_TYPE = DIRECTORY_PREFIX + DirectoryRadicals.TYPE;

	/**
	 * @deprecated Use {@link LuceneIndexSettings#DIRECTORY_ROOT} instead.
	 */
	@Deprecated
	public static final String DIRECTORY_ROOT = DIRECTORY_PREFIX + DirectoryRadicals.ROOT;

	/**
	 * @deprecated Use {@link LuceneIndexSettings#DIRECTORY_LOCKING_STRATEGY} instead.
	 */
	@Deprecated
	public static final String DIRECTORY_LOCKING_STRATEGY =
			DIRECTORY_PREFIX + DirectoryRadicals.LOCKING_STRATEGY;

	/**
	 * @deprecated Use {@link LuceneIndexSettings#DIRECTORY_FILESYSTEM_ACCESS_STRATEGY} instead.
	 */
	@Deprecated
	public static final String DIRECTORY_FILESYSTEM_ACCESS_STRATEGY =
			DIRECTORY_PREFIX + DirectoryRadicals.FILESYSTEM_ACCESS_STRATEGY;

	/**
	 * The multi-tenancy strategy to use.
	 * <p>
	 * Expects a {@link MultiTenancyStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_STRATEGY}.
	 */
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy.strategy";

	/**
	 * The analysis configurer to use.
	 * <p>
	 * Expects a reference to a bean of type {@link LuceneAnalysisConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String ANALYSIS_CONFIGURER = "analysis.configurer";

	/**
	 * The size of the thread pool assigned to the backend.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to the number of processor cores available to the JVM on startup.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Threads",
	 * for more information about this setting and its implications.
	 */
	public static final String THREAD_POOL_SIZE = "thread_pool.size";

	/**
	 * @deprecated Use {@link LuceneIndexSettings.DirectoryRadicals} instead.
	 */
	@Deprecated
	public static final class DirectoryRadicals {

		private DirectoryRadicals() {
		}

		public static final String TYPE = "type";
		public static final String ROOT = "root";
		public static final String LOCKING_STRATEGY = "locking.strategy";
		public static final String FILESYSTEM_ACCESS_STRATEGY = "filesystem_access.strategy";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final Version LUCENE_VERSION = Version.LATEST;

		/**
		 * @deprecated Use {@link LuceneIndexSettings.Defaults#DIRECTORY_TYPE} instead.
		 */
		@Deprecated
		public static final String DIRECTORY_TYPE = "local-filesystem";

		/**
		 * @deprecated Use {@link LuceneIndexSettings.Defaults#DIRECTORY_ROOT} instead.
		 */
		@Deprecated
		public static final String DIRECTORY_ROOT = ".";

		/**
		 * @deprecated Use {@link LuceneIndexSettings.Defaults#DIRECTORY_FILESYSTEM_ACCESS_STRATEGY} instead.
		 */
		@Deprecated
		public static final FileSystemAccessStrategyName DIRECTORY_FILESYSTEM_ACCESS_STRATEGY =
				FileSystemAccessStrategyName.AUTO;

		public static final MultiTenancyStrategyName MULTI_TENANCY_STRATEGY = MultiTenancyStrategyName.NONE;
	}
}
