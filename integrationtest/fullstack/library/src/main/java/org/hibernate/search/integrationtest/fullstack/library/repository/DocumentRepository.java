/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repository;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.integrationtest.fullstack.library.model.Book;
import org.hibernate.search.integrationtest.fullstack.library.model.BookCopy;
import org.hibernate.search.integrationtest.fullstack.library.model.BookMedium;
import org.hibernate.search.integrationtest.fullstack.library.model.Document;
import org.hibernate.search.integrationtest.fullstack.library.model.ISBN;
import org.hibernate.search.integrationtest.fullstack.library.model.Library;
import org.hibernate.search.integrationtest.fullstack.library.model.LibraryService;
import org.hibernate.search.integrationtest.fullstack.library.model.Video;
import org.hibernate.search.integrationtest.fullstack.library.model.VideoCopy;
import org.hibernate.search.integrationtest.fullstack.library.model.VideoMedium;
import org.hibernate.search.engine.spatial.GeoPoint;

public abstract class DocumentRepository {

	// Hack to deal with Document<?> instead of raw Document
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static final Class<Document<?>> DOCUMENT_CLASS = (Class<Document<?>>) (Class) Document.class;

	protected final FullTextEntityManager entityManager;

	public DocumentRepository(EntityManager entityManager) {
		this.entityManager = Search.getFullTextEntityManager( entityManager );
	}

	public Book createBook(int id, ISBN isbn, String title, String summary, String tags) {
		Book book = new Book();
		book.setId( id );
		book.setIsbn( isbn );
		book.setTitle( title );
		book.setSummary( summary );
		book.setTags( tags );
		entityManager.persist( book );
		return book;
	}

	public Video createVideo(int id, String title, String summary, String tags) {
		Video video = new Video();
		video.setId( id );
		video.setTitle( title );
		video.setSummary( summary );
		video.setTags( tags );
		entityManager.persist( video );
		return video;
	}

	public BookCopy createCopy(Library library, Book document, BookMedium medium) {
		BookCopy copy = new BookCopy();
		copy.setLibrary( library );
		library.getCopies().add( copy );
		copy.setDocument( document );
		document.getCopies().add( copy );
		copy.setMedium( medium );
		entityManager.persist( copy );
		return copy;
	}

	public VideoCopy createCopy(Library library, Video document, VideoMedium medium) {
		VideoCopy copy = new VideoCopy();
		copy.setLibrary( library );
		library.getCopies().add( copy );
		copy.setDocument( document );
		document.getCopies().add( copy );
		copy.setMedium( medium );
		entityManager.persist( copy );
		return copy;
	}

	public abstract Optional<Book> getByIsbn(String isbnAsString);

	public abstract long count();

	public abstract List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit);

	public abstract List<Document<?>> searchAroundMe(String terms, String tags,
			GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryService> libraryServices,
			int offset, int limit);

}
