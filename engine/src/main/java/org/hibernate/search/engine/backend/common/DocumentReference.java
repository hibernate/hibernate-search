/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.common;

/**
 * A reference to an indexed document.
 */
public interface DocumentReference {

	/**
	 * @return The name of the index hosting the referenced document.
	 * The name is returned as it is configured in Hibernate Search,
	 * i.e. it is <strong>not</strong> normalized (lowercased, ...) as some backends may do under the hood.
	 */
	String getIndexName();

	/**
	 * @return The identifier of the referenced document.
	 * The identifier is returned as it was generated during document building,
	 * i.e. it does <strong>not</strong> take into account backend-specific transformations
	 * such as appending a tenant ID when using multi-tenancy.
	 */
	String getId();

}
