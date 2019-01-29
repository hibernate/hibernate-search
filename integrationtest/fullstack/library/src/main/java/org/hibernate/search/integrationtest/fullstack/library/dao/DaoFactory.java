/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.dao;

import javax.persistence.EntityManager;

public interface DaoFactory {

	DocumentDao createDocumentDao(EntityManager entityManager);

	LibraryDao createLibraryDao(EntityManager entityManager);

	PersonDao createPersonDao(EntityManager entityManager);

}
