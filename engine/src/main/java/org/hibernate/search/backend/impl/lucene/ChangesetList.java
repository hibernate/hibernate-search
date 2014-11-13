/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.LuceneWork;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregator for {@link org.hibernate.search.backend.impl.lucene.Changeset}
 *
 * @author gustavonalle
 */
final class ChangesetList {

	private final List<Changeset> changesets;

	ChangesetList(List<Changeset> changesets) {
		this.changesets = changesets;
	}

	List<LuceneWork> getWork() {
		ArrayList<LuceneWork> luceneWorks = new ArrayList<>();
		for ( Changeset changeset : changesets ) {
			luceneWorks.addAll( changeset.getWorkList() );
		}
		return luceneWorks;
	}

	void markProcessed() {
		for ( Changeset changeset : changesets ) {
			changeset.markProcessed();
		}
	}

}
