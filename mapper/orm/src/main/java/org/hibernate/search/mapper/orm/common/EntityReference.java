/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common;

/**
 * The (legacy) EntityReference interface specific to the Hibernate ORM mapper.
 *
 * @deprecated Use {@link org.hibernate.search.engine.common.EntityReference} instead.
 */
@Deprecated
public interface EntityReference extends org.hibernate.search.engine.common.EntityReference {

	/**
	 * @return The name of the referenced entity in the Hibernate ORM mapping.
	 * @see javax.persistence.Entity#name()
	 */
	@Override
	String name();

}
