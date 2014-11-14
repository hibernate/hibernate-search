/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Collections;
import java.util.Iterator;

import org.hibernate.search.backend.LuceneWork;

/**
 * Aggregator for {@link org.hibernate.search.backend.impl.lucene.Changeset}
 *
 * @author gustavonalle
 */
final class ChangesetList implements Iterable<LuceneWork> {

	private final Iterable<Changeset> changesets;

	ChangesetList(Iterable<Changeset> changesets) {
		this.changesets = changesets;
	}

	void markProcessed() {
		for ( Changeset changeset : changesets ) {
			changeset.markProcessed();
		}
	}

	@Override
	public Iterator<LuceneWork> iterator() {
		return new WorkIterable( changesets.iterator() );
	}

	/**
	 * A shallow iterator on all LuceneWork which avoids collection copies.
	 * Optimized as this code area is very hot at runtime.
	 */
	private static class WorkIterable implements Iterator<LuceneWork> {

		private Iterator<Changeset> outherIterator;
		private Iterator<LuceneWork> current = Collections.<LuceneWork>emptyIterator();

		public WorkIterable(Iterator<Changeset> iterator) {
			this.outherIterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return current.hasNext() || outherIterator.hasNext();
		}

		@Override
		public LuceneWork next() {
			if ( current.hasNext() ) {
				//advance the inner loop only
				return current.next();
			}
			else {
				//advance outher loop first
				Changeset next = outherIterator.next();
				current = next.getWorkListIterator();
				return current.next();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "This iterator is unable to remove elements" );
		}

	}

}
