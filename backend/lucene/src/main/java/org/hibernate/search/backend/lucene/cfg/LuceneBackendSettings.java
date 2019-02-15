/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

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

	// TODO HSEARCH-3440 document this property
	public static final String DIRECTORY_PROVIDER = "directory_provider";

	// TODO HSEARCH-3440 document this property
	public static final String ROOT_DIRECTORY = "root_directory";

	/**
	 * The multi-tenancy strategy to use.
	 * <p>
	 * Expects a {@link MultiTenancyStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_STRATEGY}.
	 */
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy_strategy";

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
	public static final String ANALYSIS_CONFIGURER = "analysis_configurer";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final Version LUCENE_VERSION = Version.LATEST;

		public static final MultiTenancyStrategyName MULTI_TENANCY_STRATEGY = MultiTenancyStrategyName.NONE;
	}
}
