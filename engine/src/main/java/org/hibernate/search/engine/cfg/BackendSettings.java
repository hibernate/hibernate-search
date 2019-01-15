/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

/**
 * Configuration properties common to all Hibernate Search backends regardless of the underlying technology.
 */
public final class BackendSettings {

	private BackendSettings() {
	}

	/**
	 * The type of the backend.
	 * <p>
	 * Should generally be provided as a String, such as "lucene" or "elasticsearch".
	 * See the documentation of your backend to find the appropriate value.
	 */
	public static final String TYPE = "type";

	/**
	 * The root property whose children are default properties to be applied to all indexes of this backend.
	 */
	public static final String INDEX_DEFAULTS = "index_defaults";

}
