/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * A custom structure similar to a concurrent linked list.
 *
 * This could be functionally replaced by a LinkedBlockingDeque, but we only
 * need a specific subset of its functionality.
 * Specifically, we need to maintain the order of elements being added, but on
 * a drain we'll only ever need to iterate the list sequentially, and the
 * drain needs to atomically reset the queue.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 * @since 5.0
 */
public final class MultiWriteDrainableLinkedList<T> {

	//Guarded by synchronization on this
	private Node<T> first = null;

	//Guarded by synchronization on this
	private Node<T> last = null;

	/**
	 * Adds a new entry to this list.
	 *
	 * @param element The element to add.
	 */
	public void add(T element) {
		final Node<T> newnode = new Node<T>( element );
		addNode( newnode );
	}

	private synchronized void addNode(Node<T> newnode) {
		if ( first == null ) {
			first = newnode;
			last = newnode;
		}
		else {
			last.next = newnode;
			last = newnode;
		}
	}

	/**
	 * Returns an Iterable over all results added so far, but
	 * atomically clears the structure as well.
	 * The returned iterable will be the only entry point to
	 * read the previously appended data.
	 * @return an Iterable, or null if there is no data.
	 */
	public Iterable<T> drainToDetachedIterable() {
		final Node<T> head = drainHead();
		if ( head != null ) {
			return new DetachedNodeIterable<T>( head );
		}
		else {
			//The choice to return null rather than an empty iterator
			//allows the client to not need an isEmpty() method, which would
			//need a different level of lock granularity.
			return null;
		}
	}

	private synchronized Node<T> drainHead() {
		final Node<T> head = first;
		first = null;
		last = null;
		return head;
	}

	static final class Node<T> {
		final T value;
		Node<T> next;
		Node(T x) {
			value = x;
		}
	}

	static final class DetachedNodeIterable<T> implements Iterable<T> {

		private final Node<T> head;

		public DetachedNodeIterable(Node<T> head) {
			this.head = head;
		}

		@Override
		public Iterator<T> iterator() {
			return new DetachedNodeIterator<T>( head );
		}
	}

	static final class DetachedNodeIterator<T> implements Iterator<T> {

		private Node<T> current;

		DetachedNodeIterator(Node<T> head) {
			this.current = head;
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public T next() {
			if ( current == null ) {
				throw new NoSuchElementException();
			}
			T v = current.value;
			current = current.next;
			return v;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "This iterator is unable to remove elements" );
		}
	}

}
