/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repo;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.hibernate.search.integrationtest.fullstack.library.cdi.EntityManagerWrapper;
import org.hibernate.search.integrationtest.fullstack.library.cdi.ManualIndexingEM;
import org.hibernate.search.integrationtest.fullstack.library.model.Book;

@ApplicationScoped
@Transactional
public class DocumentRepo implements EntityManagerProvider {

	@Inject
	@ManualIndexingEM
	private EntityManagerWrapper entityManager;

	public Book createBook(Book book) {
		entityManager.entityManager().persist( book );
		return book;
	}

	public List<Book> createBooks(List<Book> books) {
		for ( Book book : books ) {
			entityManager.entityManager().persist( book );
		}
		return books;
	}

	@Override
	public EntityManager entityManager() {
		return entityManager.entityManager();
	}
}
