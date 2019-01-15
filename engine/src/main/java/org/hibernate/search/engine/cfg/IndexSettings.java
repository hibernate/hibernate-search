/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

/**
 * Configuration properties common to all Hibernate Search indexes regardless of the underlying technology.
 */
public final class IndexSettings {

	private IndexSettings() {
	}

	/**
	 * The name of the backend to create the index in.
	 */
	public static final String BACKEND = "backend";

}
