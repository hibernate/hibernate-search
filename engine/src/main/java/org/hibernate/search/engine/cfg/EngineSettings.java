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
	 * The name of the default backend to use when none is defined in the index configuration.
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no value, meaning a backend must be {@link IndexSettings#BACKEND set for every single index}.
	 */
	public static final String DEFAULT_BACKEND = "default_backend";

	/**
	 * The root property whose children are backend names, e.g. "backends.myBackend.type = elasticsearch".
	 */
	public static final String BACKENDS = "backends";

	/**
	 * The root property whose children are index names, e.g. "indexes.myIndex.backend = myBackend".
	 */
	public static final String INDEXES = "indexes";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

	}
}
