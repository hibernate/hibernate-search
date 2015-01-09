/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

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
		return new WorkIterator( changesets.iterator() );
	}

	/**
	 * A shallow iterator on all LuceneWork which avoids collection copies.
	 * Optimized as this code area is very hot at runtime.
	 */
	private static class WorkIterator implements Iterator<LuceneWork> {

		private Iterator<Changeset> outerIterator;
		private Iterator<LuceneWork> current = Collections.<LuceneWork>emptyList().iterator();

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

	List<LuceneWork> copyToList() {
		List<LuceneWork> list = new LinkedList<LuceneWork>();
		for ( LuceneWork lw : this ) {
			list.add( lw );
		}
		return list;
	}

}
