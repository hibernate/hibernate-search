/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;

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
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;

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

	@Autowired
	private EntityManager entityManager;

	public Library create(int id, String name, int collectionSize, double latitude, double longitude, LibraryServiceOption... services) {
		return libraryRepo.save( new Library( id, name, collectionSize, latitude, longitude, services ) );
	}

	public BookCopy createCopyInLibrary(Library library, Book book, BookMedium medium) {
		return copyRepo.save( library.add( book, medium ) );
	}

	public VideoCopy createCopyInLibrary(Library library, Video video, VideoMedium medium) {
		return copyRepo.save( library.add( video, medium ) );
	}

	public List<Library> search(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		FullTextQuery<Library> query = Search.getFullTextEntityManager( entityManager )
				.search( Library.class ).query()
				.asEntity()
				.predicate( f -> f.match().onField( "name" ).matching( terms ) )
				.sort( c -> {
					c.byField( "collectionSize" ).desc();
					c.byField( "name_sort" );
				} )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
