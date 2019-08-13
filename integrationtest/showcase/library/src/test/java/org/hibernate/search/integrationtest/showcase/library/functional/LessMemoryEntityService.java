/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.functional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.integrationtest.showcase.library.repository.DocumentCopyRepository;
import org.hibernate.search.integrationtest.showcase.library.repository.DocumentRepository;
import org.hibernate.search.integrationtest.showcase.library.repository.LibraryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Similar to {@link org.hibernate.search.integrationtest.showcase.library.service.LibraryService} and
 * {@link org.hibernate.search.integrationtest.showcase.library.service.DocumentService}.
 * <p>
 * But it applies also a <b>flush</b> and a <b>clear</b> on {@link EntityManager} and an <b>evictAll</b> on {@link EntityManagerFactory#getCache()}
 * before every commit.
 * <p>
 * This strategy can be useful for a batch processing to release memory.
 */
@Service
@Transactional
public class LessMemoryEntityService {

	@Autowired
	private LibraryRepository libraryRepo;

	@Autowired
	private DocumentRepository documentRepo;

	@Autowired
	private DocumentCopyRepository copyRepo;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private EntityManager entityManager;

	public Library createLibrary(int id, String name, int collectionSize, double latitude, double longitude, LibraryServiceOption... services) {
		Library library = libraryRepo.save( new Library( id, name, collectionSize, latitude, longitude, services ) );

		clearAll();
		return library;
	}

	public Book createBook(int id, String isbn, String title, String author, String summary, String tags) {
		Book book = documentRepo.save( new Book( id, isbn, title, author, summary, tags ) );

		clearAll();
		return book;
	}

	public BookCopy createCopyInLibrary(Library library, Book book, BookMedium medium) {
		BookCopy copy = new BookCopy();
		copy.setLibrary( library );
		library.getCopies().add( copy );
		copy.setDocument( book );
		book.getCopies().add( copy );
		copy.setMedium( medium );
		BookCopy bookCopy = copyRepo.save( copy );

		clearAll();
		return bookCopy;
	}
	private void clearAll() {
		entityManager.flush();
		entityManager.clear();
		entityManagerFactory.getCache().evictAll();
	}
}
