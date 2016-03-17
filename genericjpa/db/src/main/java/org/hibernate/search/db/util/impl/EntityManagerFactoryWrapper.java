/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

/**
 * This interface is used so we can have an abstraction from the actual ORM used for the async backend.
 * This is needed because the async backend is intended to be usable with other JPA providers than
 * Hibernate ORM as well. We also cannot be sure whether Hibernate ORM is used in conjunction with JPA
 * so we cannot rely on these interfaces, either.
 *
 * Only methods that are required for the async backend can be found in this class
 *
 * @author Martin Braun
 */
public interface EntityManagerFactoryWrapper {

	EntityManagerWrapper createEntityManager();

	boolean isOpen();

}
