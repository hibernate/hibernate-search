/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether index-time scoring information for the field should be stored or not.
 * <p>
 * Enabling norms will improve the quality of scoring.
 * Disabling norms will reduce the disk space used by the index.
 */
public enum Norms {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * The index-time scoring information is not stored.
	 */
	NO,
	/**
	 * The index-time scoring information is stored.
	 */
	YES
}
