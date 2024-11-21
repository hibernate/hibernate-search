/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.cfg;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurer;
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
	 * The value to set the {@link org.hibernate.search.engine.cfg.BackendSettings#TYPE backend type}
	 * configuration property to
	 * in order to get a Lucene backend instantiated by Hibernate Search.
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
	 * Expects a Lucene {@link org.apache.lucene.util.Version} object,
	 * or a String accepted by {@link org.apache.lucene.util.Version#parseLeniently(java.lang.String)}
	 * <p>
	 * Defaults to {@link Defaults#LUCENE_VERSION}, which may change when Hibernate Search or Lucene is upgraded,
	 * and therefore does not offer any backwards-compatibility guarantees.
	 * The recommended approach is to set this property explicitly to the version of Lucene you want to target.
	 */
	public static final String LUCENE_VERSION = "lucene_version";

	/**
	 * How to implement multi-tenancy.
	 * <p>
	 * Expects a {@link MultiTenancyStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_STRATEGY}.
	 */
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy.strategy";

	/**
	 * The configurer for analysis.
	 * <p>
	 * Expects a single-valued or multi-valued reference to beans of type {@link LuceneAnalysisConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "multi-valued bean reference" properties and accepted values.
	 */
	public static final String ANALYSIS_CONFIGURER = "analysis.configurer";

	/**
	 * The configurer for query caching.
	 * <p>
	 * Expects a single-valued or multi-valued reference to beans of type {@link QueryCachingConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "multi-valued bean reference" properties and accepted values.
	 */
	public static final String QUERY_CACHING_CONFIGURER = "query.caching.configurer";

	/**
	 * The size of the thread pool assigned to the backend.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed into an integer value.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Threads",
	 * for more information about this setting and its implications.
	 * <p>
	 * Defaults to the number of processor cores available to the JVM on startup.
	 */
	public static final String THREAD_POOL_SIZE = "thread_pool.size";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final Version LUCENE_VERSION = Version.LATEST;

		/**
		 * @deprecated The default for this property is now dynamic and depends on the mapper.
		 * If the multi-tenancy is enabled in the mapper, the default is {@link MultiTenancyStrategyName#DISCRIMINATOR}.
		 * Otherwise, the default is still {@link MultiTenancyStrategyName#NONE}.
		 */
		@Deprecated
		public static final MultiTenancyStrategyName MULTI_TENANCY_STRATEGY = MultiTenancyStrategyName.NONE;
	}
}
