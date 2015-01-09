/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hibernate.search.backend.LuceneWork;

/**
 * Aggregator for {@link org.hibernate.search.backend.impl.lucene.Changeset}
 *
 * @author gustavonalle
 */
public final class ChangesetList implements Iterable<LuceneWork> {

	private final Iterable<Changeset> changesets;

	public ChangesetList(Iterable<Changeset> changesets) {
		this.changesets = changesets;
	}

	void markProcessed() {
		for ( Changeset changeset : changesets ) {
			changeset.markProcessed();
		}
	}

	@Override
	public Iterator<LuceneWork> iterator() {
		return new WorkIterator( changesets.iterator() );
	}

	/**
	 * A shallow iterator on all LuceneWork which avoids collection copies.
	 * Optimized as this code area is very hot at runtime.
	 */
	private static class WorkIterator implements Iterator<LuceneWork> {

		private Iterator<Changeset> outerIterator;
		private Iterator<LuceneWork> current = Collections.<LuceneWork>emptyIterator();

		public WorkIterator(Iterator<Changeset> iterator) {
			this.outerIterator = iterator;
		}

		@Override
		public boolean hasNext() {
			// advance the outer until we find a non empty current or we reach the end of the outer
			// to work around empty LuceneWork lists being passed
			while ( ! current.hasNext() && outerIterator.hasNext() ) {
				current = outerIterator.next().getWorkListIterator();
			}
			return current.hasNext() || outerIterator.hasNext();
		}

		@Override
		public LuceneWork next() {
			// force the position to an non empty current or the end of the flow
			if ( ! hasNext() ) {
				throw new NoSuchElementException( "Reached the end of the ChangesetList. Make sure to guard .next() with .hasNext()" );
			}
			return current.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "This iterator is unable to remove elements" );
		}

	}

}
