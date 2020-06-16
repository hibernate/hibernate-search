/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * Defines how the structure of an object field is preserved upon indexing.
 *
 * @deprecated Use {@link org.hibernate.search.engine.backend.types.ObjectStructure} instead.
 */
@Deprecated
public enum ObjectFieldStorage {

	/**
	 * @deprecated Use {@link org.hibernate.search.engine.backend.types.ObjectStructure#DEFAULT}
	 * @see org.hibernate.search.engine.backend.types.ObjectStructure#DEFAULT
	 */
	@Deprecated
	DEFAULT,
	/**
	 * @deprecated Use {@link org.hibernate.search.engine.backend.types.ObjectStructure#FLATTENED}
	 * @see org.hibernate.search.engine.backend.types.ObjectStructure#FLATTENED
	 */
	@Deprecated
	FLATTENED,
	/**
	 * @deprecated Use {@link org.hibernate.search.engine.backend.types.ObjectStructure#NESTED}
	 * @see org.hibernate.search.engine.backend.types.ObjectStructure#NESTED
	 */
	@Deprecated
	NESTED

}
