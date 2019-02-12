/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoCopy;
import org.hibernate.search.integrationtest.showcase.library.model.VideoMedium;
import org.hibernate.search.integrationtest.showcase.library.repository.DocumentCopyRepository;
import org.hibernate.search.integrationtest.showcase.library.repository.LibraryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LibraryService {

	@Autowired
	private LibraryRepository libraryRepo;

	@Autowired
	private DocumentCopyRepository copyRepo;

	public Library create(int id, String name, int collectionSize, double latitude, double longitude, LibraryServiceOption... services) {
		return libraryRepo.save( new Library( id, name, collectionSize, latitude, longitude, services ) );
	}

	public BookCopy createCopyInLibrary(Library library, Book book, BookMedium medium) {
		BookCopy copy = new BookCopy();
		copy.setLibrary( library );
		library.getCopies().add( copy );
		copy.setDocument( book );
		book.getCopies().add( copy );
		copy.setMedium( medium );
		return copyRepo.save( copy );
	}

	public VideoCopy createCopyInLibrary(Library library, Video video, VideoMedium medium) {
		VideoCopy copy = new VideoCopy();
		copy.setLibrary( library );
		library.getCopies().add( copy );
		copy.setDocument( video );
		video.getCopies().add( copy );
		copy.setMedium( medium );
		return copyRepo.save( copy );
	}

	public List<Library> search(String terms, int offset, int limit) {
		return libraryRepo.search( terms, offset, limit );
	}
}
