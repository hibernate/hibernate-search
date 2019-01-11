/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.dao;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.integrationtest.fullstack.library.model.Library;
import org.hibernate.search.integrationtest.fullstack.library.model.LibraryService;

public abstract class LibraryDao {

	protected final FullTextEntityManager entityManager;

	public LibraryDao(EntityManager entityManager) {
		this.entityManager = Search.getFullTextEntityManager( entityManager );
	}

	public Library create(int id, String name, int collectionSize, double latitude, double longitude, LibraryService... services) {
		Library library = new Library();
		library.setId( id );
		library.setName( name );
		library.setCollectionSize( collectionSize );
		library.setLatitude( latitude );
		library.setLongitude( longitude );
		library.setServices( Arrays.asList( services ) );
		entityManager.persist( library );
		return library;
	}

	public abstract List<Library> search(String terms, int offset, int limit);

}
