/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * A very simple, immutable data structure to represent singly linked lists.
 *
 * @param <T> The type of values stored in the list.
 */
public final class LinkedNode<T> implements Iterable<T> {
	public static <T> LinkedNode<T> of(T value) {
		return new LinkedNode<>( value, null );
	}

	@SafeVarargs
	public static <T> LinkedNode<T> of(T... values) {
		Contracts.assertNotNullNorEmpty( values, "values" );
		LinkedNode<T> tail = null;
		for ( int i = values.length - 1; i >= 0; i-- ) {
			tail = new LinkedNode<>( values[i], tail );
		}
		return tail;
	}

	public final T value;
	private final LinkedNode<T> tail;

	// For quick access
	public final LinkedNode<T> last;

	private LinkedNode(T value, LinkedNode<T> tail) {
		this.value = value;
		this.tail = tail;
		this.last = tail == null ? this : tail.last;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( '[' );
		boolean first = true;
		for ( Iterator<T> it = iterator(); it.hasNext(); ) {
			if ( first ) {
				first = false;
			}
			else {
				sb.append( " => " );
			}
			sb.append( it.next() );
		}
		return sb.append( ']' ).toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof LinkedNode ) ) {
			return false;
		}
		LinkedNode<?> other = (LinkedNode<?>) obj;
		return Objects.equals( value, other.value ) && Objects.equals( tail, other.tail );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value, tail );
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private LinkedNode<T> next = LinkedNode.this;

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public T next() {
				if ( next == null ) {
					throw new NoSuchElementException();
				}
				T value = next.value;
				next = next.tail;
				return value;
			}
		};
	}

	public LinkedNode<T> withHead(T headValue) {
		return new LinkedNode<>( headValue, this );
	}
}
