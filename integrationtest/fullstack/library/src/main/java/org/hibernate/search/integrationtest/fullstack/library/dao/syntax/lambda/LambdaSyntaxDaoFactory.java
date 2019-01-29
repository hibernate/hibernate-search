/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.dao.syntax.lambda;

import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.fullstack.library.dao.DaoFactory;
import org.hibernate.search.integrationtest.fullstack.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.fullstack.library.dao.LibraryDao;
import org.hibernate.search.integrationtest.fullstack.library.dao.PersonDao;

public class LambdaSyntaxDaoFactory implements DaoFactory {
	@Override
	public DocumentDao createDocumentDao(EntityManager entityManager) {
		return new LambdaSyntaxDocumentDao( entityManager );
	}

	@Override
	public LibraryDao createLibraryDao(EntityManager entityManager) {
		return new LambdaSyntaxLibraryDao( entityManager );
	}

	@Override
	public PersonDao createPersonDao(EntityManager entityManager) {
		return new LambdaSyntaxPersonDao( entityManager );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
