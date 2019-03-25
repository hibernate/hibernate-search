/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

/**
 * Configuration properties common to all Hibernate Search indexes regardless of the underlying technology.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key.
 * The exact prefix will depend on the integration, but should generally look like
 * either "{@code hibernate.search.indexes.<index name>.}" (for per-index settings)
 * or "{@code hibernate.search.backends.<backend name>.index_defaults.}" (for default index settings).
 */
public final class IndexSettings {

	private IndexSettings() {
	}

}
