/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

	@Override
	public Spliterator<T> spliterator() {
		return Spliterators.spliteratorUnknownSize( iterator(), Spliterator.IMMUTABLE | Spliterator.ORDERED );
	}

	public Stream<T> stream() {
		return StreamSupport.stream( spliterator(), false );
	}

	public LinkedNode<T> withHead(T headValue) {
		return new LinkedNode<>( headValue, this );
	}

	/**
	 * @param valuePredicate A predicate to apply to node values.
	 * @return An optional containing the path from the found node to the current head,
	 * i.e. a reversed list of all values
	 * from the first node to match the given predicate to the current head
	 * (note: the list is purposely in reversed order compared to {@code this}),
	 * or an empty optional if no matching value was found.
	 */
	public Optional<LinkedNode<T>> findAndReverse(Predicate<T> valuePredicate) {
		return findAndReverse( valuePredicate, this );
	}

	public Optional<LinkedNode<T>> findAndReverse(Predicate<T> valuePredicate, LinkedNode<T> head) {
		if ( valuePredicate.test( value ) ) {
			return Optional.of( head.reverse( null, this ) );
		}
		else if ( tail != null ) {
			return tail.findAndReverse( valuePredicate, head );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * @param newTail The tail of the new "reversed" node.
	 * @param lastIncludedNode The last node to include in the reversed list;
	 * must be in the tail of {@code this}.
	 * @return A list including all values from {@code lastNode} to {@code this},
	 * in reversed order.
	 */
	private LinkedNode<T> reverse(LinkedNode<T> newTail, LinkedNode<T> lastIncludedNode) {
		LinkedNode<T> thisWithNewTail = new LinkedNode<>( value, newTail );
		if ( lastIncludedNode == this ) {
			return thisWithNewTail;
		}
		else {
			return tail.reverse( thisWithNewTail, lastIncludedNode );
		}
	}
}
