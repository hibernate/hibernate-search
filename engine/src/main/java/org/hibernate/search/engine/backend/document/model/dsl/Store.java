/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;


/**
 * Whether or not the value is stored in the document
 *
 * @author Emmanuel Bernard
 */
public enum Store {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * does not store the value in the index
	 */
	NO,
	/**
	 * stores the value in the index
	 */
	YES,
	/**
	 * stores the value in the index in a compressed form
	 */
	COMPRESS
}
