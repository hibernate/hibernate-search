/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class SuppressingCloserTest {

	@Test
	void nullCloseable() {
		Throwable mainException = new Exception();

		// Should not do anything, in particular should not throw any NPE
		new SuppressingCloser( mainException ).push( null );
		new SuppressingCloser( mainException ).pushAll( new Closeable[] { null } );
		new SuppressingCloser( mainException ).pushAll( Arrays.asList( (Closeable) null ) );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	void extract_nullSupplier() {
		Throwable mainException = new Exception();

		// Should not do anything, in particular should not throw any NPE
		new SuppressingCloser( mainException ).push( Closeable::close, (Supplier<Closeable>) null, Supplier::get );
		new SuppressingCloser( mainException ).pushAll( Closeable::close, Arrays.asList( (Supplier<Closeable>) null ),
				Supplier::get );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	void extract_nullCloseable() {
		Throwable mainException = new Exception();

		// Should not do anything, in particular should not throw any NPE
		new SuppressingCloser( mainException ).push( Closeable::close, (Supplier<Closeable>) () -> null, Supplier::get );
		new SuppressingCloser( mainException ).pushAll( Closeable::close,
				Arrays.asList( (Supplier<Closeable>) () -> null ), Supplier::get );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	void javaIOCloseable() {
		Throwable mainException = new Exception();
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		Closeable closeable = () -> {
			throw exception1;
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	void extract_javaIOCloseable() {
		Throwable mainException = new Exception();
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		Closeable closeable = () -> {
			throw exception1;
		};
		Supplier<Closeable> supplier = () -> closeable;


		new SuppressingCloser( mainException )
				.push( Closeable::close, supplier, Supplier::get )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	void autoCloseable() {
		Throwable mainException = new Exception();
		Exception exception1 = new Exception();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		AutoCloseable closeable = () -> {
			throw exception1;
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	void extract_autoCloseable() {
		Throwable mainException = new Exception();
		Exception exception1 = new Exception();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		AutoCloseable closeable = () -> {
			throw exception1;
		};
		Supplier<AutoCloseable> supplier = () -> closeable;

		new SuppressingCloser( mainException )
				.push( AutoCloseable::close, supplier, Supplier::get )
				.push( ignored -> {
					throw exception2;
				}, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 );
	}

	@Test
	void runtimeException() {
		Throwable mainException = new Exception();
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new IllegalStateException();
		RuntimeException exception3 = new UnsupportedOperationException();

		new SuppressingCloser( mainException )
				.push( ignored -> { throw exception1; }, new Object() )
				.push( ignored -> {
					throw exception2;
				}, new Object() )
				.push( ignored -> { throw exception3; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	void extract_runtimeException() {
		Throwable mainException = new Exception();
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new IllegalStateException();
		RuntimeException exception3 = new UnsupportedOperationException();

		Supplier<Object> supplier1 = () -> { throw exception1; };
		Supplier<Object> supplier2 = () -> { throw exception2; };
		Supplier<Object> supplier3 = () -> new Object();

		new SuppressingCloser( mainException )
				.push( ignored -> fail( "Should not be called" ), supplier1, Supplier::get )
				.push( ignored -> fail( "Should not be called" ), supplier2, Supplier::get )
				.push( ignored -> {
					throw exception3;
				}, supplier3, Supplier::get );

		assertThat( mainException )
				.hasSuppressedException( exception1 )
				.hasSuppressedException( exception2 )
				.hasSuppressedException( exception3 );
	}

	@Test
	void someNonFailingCloseables() {
		Throwable mainException = new Exception();
		RuntimeException exception1 = new RuntimeException();
		RuntimeException exception2 = new RuntimeException();
		RuntimeException exception3 = new RuntimeException();

		new SuppressingCloser( mainException )
				.push( () -> { /* Do not fail */ } )
				.push( ignored -> { throw exception1; }, new Object() )
				.push( ignored -> {
					throw exception2;
				}, new Object() )
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
	void onlyNonFailingCloseables() {
		Throwable mainException = new Exception();

		new SuppressingCloser( mainException )
				.push( () -> { /* Do not fail */ } )
				.push( () -> { /* Do not fail */ } );

		assertThat( mainException )
				.hasNoSuppressedExceptions();
	}

	@Test
	void customCloseable() {
		Throwable mainException = new Exception();
		MyException1 exception1 = new MyException1();
		RuntimeException exception2 = new IllegalStateException();
		@SuppressWarnings("resource")
		MyException1Closeable closeable = () -> {
			throw exception1;
		};

		new SuppressingCloser( mainException )
				.push( closeable )
				.push( ignored -> { throw exception2; }, new Object() );

		assertThat( mainException )
				.hasSuppressedException( exception1 );
	}

	@Test
	void iterable() {
		Throwable mainException = new Exception();
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		IOException exception3 = new IOException();
		RuntimeException exception4 = new UnsupportedOperationException();
		List<Closeable> closeables = Arrays.asList(
				() -> { throw exception1; },
				() -> { throw exception2; },
				() -> {
					throw exception3;
				},
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

	@Test
	void extract_iterable() {
		Throwable mainException = new Exception();
		IOException exception1 = new IOException();
		RuntimeException exception2 = new IllegalStateException();
		IOException exception3 = new IOException();
		RuntimeException exception4 = new UnsupportedOperationException();
		List<Supplier<Closeable>> closeableSuppliers = Arrays.asList(
				() -> () -> { throw exception1; },
				() -> () -> {
					throw exception2;
				},
				() -> () -> { throw exception3; },
				() -> () -> { throw exception4; }
		);

		new SuppressingCloser( mainException )
				.pushAll( closeableSuppliers, Supplier::get );

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

}
