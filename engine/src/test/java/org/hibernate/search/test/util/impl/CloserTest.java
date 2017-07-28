/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.impl.Closer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
public class CloserTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void javaIOCloseable() throws IOException {
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		Closeable closeable = new Closeable() {
			@Override
			public void close() throws IOException {
				throw exception1;
			}
		};

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.build()
		);

		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( closeable::close );
			closer.push( () -> { throw exception2; } );
		}
	}

	@Test
	public void autoCloseable() throws Exception {
		Exception exception1 = new Exception();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		AutoCloseable closeable = new AutoCloseable() {
			@Override
			public void close() throws Exception {
				throw exception1;
			}
		};

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.build()
		);

		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.push( closeable::close );
			closer.push( () -> { throw exception2; } );
		}
	}

	@Test
	public void runtimeException() {
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new IllegalStateException();
		RuntimeException exception3 = new UnsupportedOperationException();

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.withSuppressed( exception3 )
				.build()
		);

		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( () -> { throw exception1; } );
			closer.push( () -> { throw exception2; } );
			closer.push( () -> { throw exception3; } );
		}
	}

	@Test
	public void nonFailingCloseables() {
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new RuntimeException();
		RuntimeException exception3 = new RuntimeException();

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.withSuppressed( exception3 )
				.build()
		);

		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( () -> { /* Do not fail */ } );
			closer.push( () -> { throw exception1; } );
			closer.push( () -> { throw exception2; } );
			closer.push( () -> { /* Do not fail */ } );
			closer.push( () -> { throw exception3; } );
			closer.push( () -> { /* Do not fail */ } );
		}
	}

	@Test
	public void customCloseable() throws MyException1 {
		MyException1 exception1 = new MyException1();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		MyException1Closeable closeable = new MyException1Closeable() {
			@Override
			public void close() throws MyException1 {
				throw exception1;
			}
		};

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.build()
		);

		try ( Closer<MyException1> closer = new Closer<>() ) {
			closer.push( closeable::close );
			closer.push( () -> { throw exception2; } );
		}
	}

	@Test
	public void iterable() throws IOException {
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

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.withSuppressed( exception3 )
				.withSuppressed( exception4 )
				.build()
		);

		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( Closeable::close, closeables );
		}
	}

	@Test
	public void split() throws IOException, MyException1, MyException2 {
		MyException2 exception1 = new MyException2();
		MyException1 exception2 = new MyException1();
		IOException exception3 = new IOException();

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.withSuppressed( exception3 )
				.build()
		);

		try ( Closer<IOException> closer1 = new Closer<>();
				Closer<MyException1> closer2 = closer1.split();
				Closer<MyException2> closer3 = closer1.split() ) {
			/*
			 * The first exception is caught by closer3,
			 * but regardless of the fact that closer3 is the third
			 * closer in the resource list, this exception should be
			 * the one that is rethrown.
			 */
			closer3.push( () -> { throw exception1; } );
			closer2.push( () -> { throw exception2; } );
			closer1.push( () -> { throw exception3; } );
		}
	}

	@Test
	public void split_transitive() throws IOException, MyException1, MyException2 {
		MyException2 exception1 = new MyException2();
		MyException1 exception2 = new MyException1();
		IOException exception3 = new IOException();

		thrown.expect(
				isException( exception1 )
				.withSuppressed( exception2 )
				.withSuppressed( exception3 )
				.build()
		);

		try ( Closer<IOException> closer1 = new Closer<>();
				Closer<MyException1> closer2 = closer1.split();
				// splitting on closer2 should be the same as splitting on closer1
				Closer<MyException2> closer3 = closer2.split() ) {
			/*
			 * The first exception is caught by closer3,
			 * but regardless of the fact that closer3 is the third
			 * closer in the resource list, this exception should be
			 * the one that is rethrown.
			 */
			closer3.push( () -> { throw exception1; } );
			closer2.push( () -> { throw exception2; } );
			closer1.push( () -> { throw exception3; } );
		}
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
