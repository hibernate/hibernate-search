/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.dto.LibraryFacetedSearchResult;
import org.hibernate.search.integrationtest.showcase.library.dto.LibrarySimpleProjection;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;

public interface IndexSearchLibraryRepository {
	List<Library> search(String terms, int offset, int limit);

	List<LibrarySimpleProjection> searchAndProject(String terms, int offset, int limit);

	List<LibrarySimpleProjection> searchAndProjectToMethodLocalClass(String terms, int offset, int limit);

	LibraryFacetedSearchResult searchFaceted(String terms,
			Integer minCollectionSize, List<LibraryServiceOption> libraryServices,
			int offset, int limit);
}
