/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
