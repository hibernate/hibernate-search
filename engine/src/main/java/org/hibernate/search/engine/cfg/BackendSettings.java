/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static java.lang.String.join;

/**
 * Configuration properties common to all Hibernate Search backends regardless of the underlying technology.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key.
 * The exact prefix will depend on the integration, but should generally look like
 * "{@code hibernate.search.backends.<backend-name>.}".
 */
public final class BackendSettings {

	private BackendSettings() {
	}

	/**
	 * The type of the backend.
	 * <p>
	 * Only useful if you have more than one backend technology in the classpath;
	 * otherwise the backend type is automatically detected.
	 * <p>
	 * Expects a String, such as "lucene" or "elasticsearch".
	 * See the documentation of your backend to find the appropriate value.
	 * <p>
	 * Defaults:
	 * <ul>
	 *     <li>If there is only one backend type in the classpath, defaults to that backend.</li>
	 *     <li>Otherwise, no default: this property must be set.</li>
	 * </ul>
	 */
	public static final String TYPE = "type";

	/**
	 * The root property whose children are index names, e.g. {@code indexes.myIndex.<some index-scoped property> = bar}.
	 */
	public static final String INDEXES = "indexes";

	/**
	 * Builds a configuration property key for the default backend, with the given radical.
	 * <p>
	 * See the javadoc of your backend for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backend.thread_pool.size}"
	 *
	 * @param radical The radical of the configuration property (see constants in
	 * 	 * {@code ElasticsearchBackendSettings}, {@code ElasticsearchBackendSettings}, etc.)
	 * @return the concatenated prefix + radical
	 */
	public static String backendKey(String radical) {
		return join( ".", EngineSettings.BACKEND, radical );
	}

	/**
	 * Builds a configuration property key for the given backend, with the given radical.
	 * <p>
	 * See the javadoc of your backend for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backend.myBackend.thread_pool.size}"
	 *
	 * @param backendName The name of the backend to configure.
	 * @param radical The radical of the configuration property (see constants in
	 * 	 * {@code ElasticsearchBackendSettings}, {@code ElasticsearchBackendSettings}, etc.)
	 * @return the concatenated prefix + backend name + radical
	 */
	public static String backendKey(String backendName, String radical) {
		if ( backendName == null ) {
			return backendKey( radical );
		}
		return join( ".", EngineSettings.BACKENDS, backendName, radical );
	}
}
