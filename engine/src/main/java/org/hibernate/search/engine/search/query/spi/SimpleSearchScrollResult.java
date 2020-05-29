/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.List;

import org.hibernate.search.engine.search.query.SearchScrollResult;

public class SimpleSearchScrollResult<H> implements SearchScrollResult<H> {

	private final boolean hasHits;
	private final List<H> hits;

	public SimpleSearchScrollResult(boolean hasHits, List<H> hits) {
		this.hasHits = hasHits;
		this.hits = hits;
	}

	@Override
	public boolean hasHits() {
		return hasHits;
	}

	@Override
	public List<H> hits() {
		return hits;
	}
}
