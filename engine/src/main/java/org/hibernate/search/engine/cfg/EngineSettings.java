/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

/**
 * Configuration properties for the Hibernate Search engine.
 */
public final class EngineSettings {

	private EngineSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = "hibernate.search.";

	/**
	 * The name of the default backend to use when none is defined in the index configuration.
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no value, meaning a backend must be set in the mapping for every single index.
	 */
	public static final String DEFAULT_BACKEND = PREFIX + Radicals.DEFAULT_BACKEND;

	/**
	 * The root property whose children are backend names, e.g. "hibernate.search.backends.myBackend.type = elasticsearch".
	 */
	public static final String BACKENDS = PREFIX + Radicals.BACKENDS;

	/**
	 * The strategy to use when reporting the results of configuration property checking.
	 * <p>
	 * Configuration property checking will detect an configuration property that is never used,
	 * which might indicate a configuration issue.
	 * <p>
	 * Expects a {@link ConfigurationPropertyCheckingStrategyName} value,
	 * or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#CONFIGURATION_PROPERTY_CHECKING_STRATEGY}.
	 */
	public static final String CONFIGURATION_PROPERTY_CHECKING_STRATEGY =
			PREFIX + Radicals.CONFIGURATION_PROPERTY_CHECKING_STRATEGY;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}

		public static final String DEFAULT_BACKEND = "default_backend";
		public static final String BACKENDS = "backends";
		public static final String CONFIGURATION_PROPERTY_CHECKING_STRATEGY = "configuration_property_checking.strategy";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		public static final ConfigurationPropertyCheckingStrategyName CONFIGURATION_PROPERTY_CHECKING_STRATEGY =
				ConfigurationPropertyCheckingStrategyName.WARN;

		private Defaults() {
		}

	}
}
