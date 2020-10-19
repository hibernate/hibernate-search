/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.time.Duration;
import java.util.List;

import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.SearchScrollResult;

public class SimpleSearchScrollResult<H> implements SearchScrollResult<H> {

	private final SearchResultTotal resultTotal;
	private final boolean hasHits;
	private final List<H> hits;
	private final Duration took;
	private final boolean timedOut;

	public SimpleSearchScrollResult(SearchResultTotal resultTotal, boolean hasHits, List<H> hits,
			Duration took, Boolean timedOut) {
		this.resultTotal = resultTotal;
		this.hasHits = hasHits;
		this.hits = hits;
		this.took = took;
		this.timedOut = timedOut;
	}

	@Override
	public SearchResultTotal total() {
		return resultTotal;
	}

	@Override
	public boolean hasHits() {
		return hasHits;
	}

	@Override
	public List<H> hits() {
		return hits;
	}

	@Override
	public Duration took() {
		return took;
	}

	@Override
	public boolean timedOut() {
		return timedOut;
	}
}
