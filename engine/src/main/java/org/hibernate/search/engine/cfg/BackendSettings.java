/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static java.lang.String.join;

/**
 * Configuration properties common to all Hibernate Search backends regardless of the underlying technology.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key.
 * The exact prefix will depend on the integration, but should generally look like
 * "{@code hibernate.search.backends.<backend name>.}".
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
	 * No default: this property must be set.
	 */
	public static final String TYPE = "type";

	/**
	 * The root property whose children are default properties to be applied to all indexes of this backend.
	 *
	 * @deprecated To set defaults for a particular configuration property for all indexes in a backend,
	 * just set the configuration property at the backend level.
	 * E.g. instead of {@code hibernate.search.backend.index_defaults.indexing.queue_size}
	 * set {@code hibernate.search.backend.indexing.queue_size}.
	 */
	@Deprecated
	public static final String INDEX_DEFAULTS = "index_defaults";

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
