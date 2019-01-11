/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repository.impl;

import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.fullstack.library.repository.RepositoryFactory;
import org.hibernate.search.integrationtest.fullstack.library.repository.DocumentRepository;
import org.hibernate.search.integrationtest.fullstack.library.repository.LibraryRepository;
import org.hibernate.search.integrationtest.fullstack.library.repository.PersonRepository;

public class RepositoryFactoryImpl implements RepositoryFactory {
	@Override
	public DocumentRepository createDocumentRepository(EntityManager entityManager) {
		return new DocumentRepositoryImpl( entityManager );
	}

	@Override
	public LibraryRepository createLibraryRepository(EntityManager entityManager) {
		return new LibraryRepositoryImpl( entityManager );
	}

	@Override
	public PersonRepository createPersonRepository(EntityManager entityManager) {
		return new PersonRepositoryImpl( entityManager );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
