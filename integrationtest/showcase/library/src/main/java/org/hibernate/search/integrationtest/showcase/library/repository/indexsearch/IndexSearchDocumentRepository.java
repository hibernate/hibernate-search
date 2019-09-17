/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;

public interface IndexSearchDocumentRepository {

	long countIndexed();

	List<Document<?>> findAllIndexed();

	Optional<Book> getByIsbn(String isbnAsString);

	List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit);

	List<Document<?>> searchAroundMe(String terms, String tags,
			GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryServiceOption> libraryServices,
			int offset, int limit);

	List<String> getAuthorsOfBooksHavingTerms(String terms, SortOrder order);
}
