/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class CloserTest {

	@Test
	public void nullCloseable() throws IOException {
		// Should not do anything, in particular should not throw any NPE
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( Closeable::close, null );
		}
	}

	@Test
	public void javaIOCloseable() {
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		Closeable closeable = () -> {
			throw exception1;
		};

		assertThatThrownBy( () -> {
			try ( Closer<IOException> closer = new Closer<>() ) {
				closer.push( Closeable::close, closeable );
				closer.push( ignored -> { throw exception2; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	public void autoCloseable() {
		Exception exception1 = new Exception();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		AutoCloseable closeable = () -> {
			throw exception1;
		};

		assertThatThrownBy( () -> {
			try ( Closer<Exception> closer = new Closer<>() ) {
				closer.push( AutoCloseable::close, closeable );
				closer.push( ignored -> { throw exception2; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	public void runtimeException() {
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new IllegalStateException();
		RuntimeException exception3 = new UnsupportedOperationException();

		assertThatThrownBy( () -> {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( ignored -> { throw exception1; }, new Object() );
				closer.push( ignored -> { throw exception2; }, new Object() );
				closer.push( ignored -> { throw exception3; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	public void nonFailingCloseables() {
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new RuntimeException();
		RuntimeException exception3 = new RuntimeException();

		assertThatThrownBy( () -> {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( ignored -> { /* Do not fail */ }, new Object() );
				closer.push( ignored -> { throw exception1; }, new Object() );
				closer.push( ignored -> { throw exception2; }, new Object() );
				closer.push( ignored -> { /* Do not fail */ }, new Object() );
				closer.push( ignored -> { throw exception3; }, new Object() );
				closer.push( ignored -> { /* Do not fail */ }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	public void customCloseable() {
		MyException1 exception1 = new MyException1();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		MyException1Closeable closeable = () -> {
			throw exception1;
		};

		assertThatThrownBy( () -> {
			try ( Closer<MyException1> closer = new Closer<>() ) {
				closer.push( MyException1Closeable::close, closeable );
				closer.push( ignored -> { throw exception2; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	public void iterable() {
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		IOException exception3 = new IOException();
		RuntimeException exception4 = new UnsupportedOperationException();
		List<Closeable> closeables = Arrays.asList(
				() -> { throw exception1; },
				() -> { throw exception2; },
				() -> { throw exception3; },
				() -> { throw exception4; }
				);

		assertThatThrownBy( () -> {
			try ( Closer<IOException> closer = new Closer<>() ) {
				closer.pushAll( Closeable::close, closeables );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 )
				.hasSuppressedException( exception4 );
	}

	@Test
	public void split() {
		MyException1 exception1 = new MyException1();
		MyException2 exception2 = new MyException2();
		IOException exception3 = new IOException();

		assertThatThrownBy( () -> {
			try ( Closer<IOException> closer1 = new Closer<>();
					Closer<MyException1> closer2 = closer1.split();
					Closer<MyException2> closer3 = closer1.split() ) {
				/*
				 * The first exception is caught by closer2,
				 * so regardless of the fact that closer2 is the second
				 * closer in the resource list, this exception should be
				 * the one that is rethrown.
				 */
				closer2.push( ignored -> { throw exception1; }, new Object() );
				closer3.push( ignored -> { throw exception2; }, new Object() );
				closer1.push( ignored -> { throw exception3; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	public void split_transitive() {
		MyException1 exception1 = new MyException1();
		MyException2 exception2 = new MyException2();
		IOException exception3 = new IOException();

		assertThatThrownBy( () -> {
			try ( Closer<IOException> closer1 = new Closer<>();
					Closer<MyException1> closer2 = closer1.split();
					// splitting on closer2 should be the same as splitting on closer1
					Closer<MyException2> closer3 = closer2.split() ) {
				/*
				 * The first exception is caught by closer2,
				 * so regardless of the fact that closer2 is the second
				 * closer in the resource list, this exception should be
				 * the one that is rethrown.
				 */
				closer2.push( ignored -> { throw exception1; }, new Object() );
				closer3.push( ignored -> { throw exception2; }, new Object() );
				closer1.push( ignored -> { throw exception3; }, new Object() );
			}
		} )
				.isSameAs( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	private static class MyException1 extends Exception {
	}

	private interface MyException1Closeable extends AutoCloseable {
		@Override
		void close() throws MyException1;
	}

	private static class MyException2 extends Exception {
	}

}
