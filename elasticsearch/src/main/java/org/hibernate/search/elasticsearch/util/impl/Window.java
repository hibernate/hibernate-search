/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed-size, random-access collection that removes the first element when it becomes full.
 *
 * <p>The indices accepted by this container are within a bounded range. This range may start at any
 * positive integer value, i.e. the container may not allow access to element at index {@code 0}, but
 * only indices {@code 102-134} for instance. The range will advance automatically as the window gets full
 * and more elements are added.
 *
 * @author Yoann Rodiere
 */
public class Window<E> {
	/**
	 * The external index of the first element in this window, i.e. the index that will give
	 * access to the first element when given to {@link #get(int)}.
	 * <p>If the window is empty, this will be the index of the next added element.
	 */
	private int externalStartIndex;

	/**
	 * The number of elements that can be added before the first element gets automatically erased.
	 */
	private int capacity;

	/**
	 * The initial value of {@link #externalStartIndex}, for use in {@link #clear()}.
	 */
	private final int initialExternalStartIndex;

	/**
	 * The actual data.
	 */
	private final List<E> elementData;

	/**
	 * The position where the first element in this window is stored within {{@link #elementData}.
	 * <p>If the window is empty, this will be the index of the next added element.
	 */
	private int internalStartIndex;

	/**
	 * The number of elements in this window.
	 */
	private int size;

	/**
	 * @param initialIndex The index of the first added element.
	 * @param capacity The number of elements that can be added before the first element gets automatically erased.
	 */
	public Window(int initialIndex, int capacity) {
		if ( capacity <= 0 ) {
			throw new IllegalArgumentException( "capacity must be at least 1" );
		}
		this.initialExternalStartIndex = initialIndex;
		this.capacity = capacity;
		externalStartIndex = initialIndex;
		elementData = new ArrayList<>(); // Take advantage of ArrayList's "smart" growth and O(1) random access
		internalStartIndex = 0;
		size = 0;
	}

	public int start() {
		return externalStartIndex;
	}

	/**
	 * @return The number of elements in this element container.
	 */
	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * @return The maximum number of element that can fit this window.
	 */
	public int capacity() {
		return capacity;
	}

	public E get(int externalIndex) {
		rangeCheck( externalIndex );
		int internalIndex = ( externalIndex - externalStartIndex + internalStartIndex ) % capacity;
		return elementData.get( internalIndex );
	}

	private void rangeCheck(int externalIndex) {
		int start = start();
		if ( externalIndex < start || start + size() <= externalIndex ) {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Add an element, removing the first element if it's necessary in order to respect the size limit.
	 *
	 * @param element The element to add.
	 * @return {@code true} to indicate the element has been added.
	 */
	public boolean add(E element) {
		int newElementIndex = ( internalStartIndex + size ) % capacity;
		if ( size == capacity ) {
			// Overflow: drop the first element
			internalStartIndex = ( internalStartIndex + 1 ) % capacity;
			++externalStartIndex;
		}
		else {
			++size;
		}
		if ( newElementIndex == elementData.size() ) {
			elementData.add( element );
		}
		else {
			elementData.set( newElementIndex, element );
		}
		return true;
	}

	public void clear() {
		this.externalStartIndex = initialExternalStartIndex;

		// Clear references for the GC
		for ( int i = 0; i < size; i++ ) {
			elementData.set( (internalStartIndex + i ) % capacity, null );
		}

		this.internalStartIndex = 0;
		this.size = 0;
	}
}