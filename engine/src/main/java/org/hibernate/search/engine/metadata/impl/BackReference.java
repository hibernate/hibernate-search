/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.search.exception.AssertionFailure;

/**
 * A container allowing to build metadata with circular dependency.
 *
 * <p>It's useful when building {@link DocumentFieldMetadata}, for instance:
 * when we build those, the parent type metadata hasn't been built yet (since
 * it requires every fields to be created). To break the circular dependency,
 * we provide the DocumentFieldMetadata with a {@code BackReference<TypeMetadata>},
 * and we initialize the reference later.
 *
 * @author Yoann Rodiere
 */
public class BackReference<T> {

	private static final BackReference<Object> EMPTY = new BackReference<>();
	static {
		EMPTY.initialize( null );
	}

	@SuppressWarnings("unchecked") // BackReference is covariant on T
	public static <T> BackReference<T> empty() {
		return (BackReference<T>) EMPTY;
	}

	private boolean initialized = false;
	private T value;

	public BackReference() {
	}

	public T get() {
		if ( !initialized ) {
			throw new AssertionFailure( "A reference has been accessed before having been initialized." );
		}
		return value;
	}

	void initialize(T value) {
		if ( this.value != null ) {
			throw new AssertionFailure( "A reference has been initialized more than once." );
		}
		this.value = value;
		this.initialized = true;
	}

	@Override
	public String toString() {
		if ( initialized ) {
			return String.valueOf( value );
		}
		else {
			return "<not initialized>";
		}
	}

}
