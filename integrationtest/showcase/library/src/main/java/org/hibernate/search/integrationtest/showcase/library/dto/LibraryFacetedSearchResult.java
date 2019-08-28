/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.dto;

import java.util.List;
import java.util.Map;

import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.util.common.data.Range;

public class LibraryFacetedSearchResult {

	private final long totalHitCount;
	private final List<Library> hits;

	private final Map<Range<Integer>, Long> totalHitCountByCollectionSize;
	private final Map<LibraryServiceOption, Long> totalHitCountByService;

	public LibraryFacetedSearchResult(long totalHitCount,
			List<Library> hits,
			Map<Range<Integer>, Long> totalHitCountByCollectionSize,
			Map<LibraryServiceOption, Long> totalHitCountByService) {
		this.totalHitCount = totalHitCount;
		this.hits = hits;
		this.totalHitCountByCollectionSize = totalHitCountByCollectionSize;
		this.totalHitCountByService = totalHitCountByService;
	}

	public long getTotalHitCount() {
		return totalHitCount;
	}

	public List<Library> getHits() {
		return hits;
	}

	public Map<Range<Integer>, Long> getTotalHitCountByCollectionSize() {
		return totalHitCountByCollectionSize;
	}

	public Map<LibraryServiceOption, Long> getTotalHitCountByService() {
		return totalHitCountByService;
	}
}
