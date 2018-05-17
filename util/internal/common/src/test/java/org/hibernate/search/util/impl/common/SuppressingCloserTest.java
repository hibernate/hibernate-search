/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class SuppressingCloserTest {

	@Test
	public void nullCloseable() {
		Throwable mainException = new Exception();

		// Should not do anything, in particular should not throw any NPE
		new SuppressingCloser( mainException ).push( null );
		new SuppressingCloser( mainException ).pushAll( new Closeable[] { null } );
		new SuppressingCloser( mainException ).pushAll( Arrays.asList( (Closeable) null ) );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	public void javaIOCloseable() {
		Throwable mainException = new Exception();
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		Closeable closeable = new Closeable() {
			@Override
			public void close() throws IOException {
				throw exception1;
			}
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	public void autoCloseable() {
		Throwable mainException = new Exception();
		Exception exception1 = new Exception();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		AutoCloseable closeable = new AutoCloseable() {
			@Override
			public void close() throws Exception {
				throw exception1;
			}
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	public void runtimeException() {
		Throwable mainException = new Exception();
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new IllegalStateException();
		RuntimeException exception3 = new UnsupportedOperationException();

		new SuppressingCloser( mainException )
				.push( ignored -> { throw exception1; }, new Object() )
				.push( ignored -> { throw exception2; }, new Object() )
				.push( ignored -> { throw exception3; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	public void someNonFailingCloseables() {
		Throwable mainException = new Exception();
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new RuntimeException();
		RuntimeException exception3 = new RuntimeException();

		new SuppressingCloser( mainException )
				.push( () -> { /* Do not fail */ } )
				.push( ignored -> { throw exception1; }, new Object() )
				.push( ignored -> { throw exception2; }, new Object() )
				.push( () -> { /* Do not fail */ } )
				.push( () -> { /* Do not fail */ } )
				.push( ignored -> { throw exception3; }, new Object() )
				.push( () -> { /* Do not fail */ } );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	public void onlyNonFailingCloseables() {
		Throwable mainException = new Exception();

		new SuppressingCloser( mainException )
				.push( () -> { /* Do not fail */ } )
				.push( () -> { /* Do not fail */ } );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	public void customCloseable() {
		Throwable mainException = new Exception();
		MyException1 exception1 = new MyException1();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		MyException1Closeable closeable = new MyException1Closeable() {
			@Override
			public void close() throws MyException1 {
				throw exception1;
			}
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 );
	}

	@Test
	public void iterable() throws IOException {
		Throwable mainException = new Exception();
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

		new SuppressingCloser( mainException )
				.pushAll( closeables );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 )
				.hasSuppressedException( exception4 );
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
