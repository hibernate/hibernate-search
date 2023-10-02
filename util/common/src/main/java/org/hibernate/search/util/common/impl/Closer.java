/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

/**
 * A helper for closing multiple resources and re-throwing only one exception,
 * {@link Throwable#addSuppressed(Throwable) suppressing} the others as necessary.
 * <p>
 * This class is not thread safe.
 * <p>
 * This helper is mainly useful when implementing {@link AutoCloseable#close()}
 * or a similar closing method in your own class, to make sure that all resources are
 * at least given the chance to close, even if closing one of them fails.
 * When creating then closing resources in the scope of a single method call,
 * try-with-resource blocks should be favored.
 * <p>
 * See the {@link AbstractCloser} superclass for a list of methods
 * allowing to close objects while catching exceptions.
 * <p>
 * <strong>Warning:</strong> In order not to ignore exceptions,
 * you should <strong>always</strong> call {@link Closer#close()}
 * once you closed all of your resources. The most straightforward way
 * to do this is to use a try-with-resource block:
 * <pre><code>
 * public void myCloseFunction() throws MyException {
 *   try ( Closer&lt;MyException&gt; closer = new Closer&lt;&gt;() ) {
 *     closer.push( this.myCloseable::close );
 *     closer.pushAll( this.myListOfCloseables, MyCloseableType::close );
 *   }
 * }
 * </code></pre>
 *
 * <h3>Exception type</h3>
 * <p>
 * Note that the closer has a generic type parameter, allowing it to
 * re-throw a given checked exception.
 * If you don't want to use this feature, you can simply use a
 * {@code Closer<RuntimeException>}.
 *
 * <h3><a name="splitting">Splitting</a></h3>
 * <p>
 * If you need to close multiple resources throwing different checked
 * exceptions, and those exceptions don't have a practical common superclass,
 * you can "split" the closer:
 * <pre><code>
 * public void myCloseFunction() throws IOException, MyException, MyOtherException {
 *   try ( Closer&lt;MyException&gt; closer1 = new Closer&lt;&gt;();
 *       Closer&lt;IOException&gt; closer2 = closer1.split();
 *       Closer&lt;MyOtherException&gt; closer3 = closer1.split() ) {
 *     closer2.push( this.someJavaIOCloseable::close );
 *     closer1.pushAll( this.myListOfCloseables1, MyCloseableTypeThrowingMyException::close );
 *     closer3.pushAll( this.myListOfCloseables2, MyCloseableTypeThrowingMyOtherException::close );
 *   }
 * }
 * </code></pre>
 * <p>
 * The multiple closers will share the same state, which means the first
 * exception to be caught by any of the closers will be the one to be re-thrown,
 * and all subsequent exceptions caught by any closer will be added as suppressed
 * to this first exception.
 * <strong>Be careful though, you will have to close every single closer.</strong>
 * Closing just the original one will not be enough.
 *
 * @param <E> The supertype of exceptions this closer can catch and re-throw,
 * besides {@link RuntimeException} and {@link Error}.
 *
 */
public final class Closer<E extends Exception> extends AbstractCloser<Closer<E>, E> implements AutoCloseable {

	private final CloseableState state;

	public Closer() {
		this( new CloseableState() );
	}

	private Closer(CloseableState state) {
		this.state = state;
	}

	/**
	 * @return A closer sharing the same state as {@code this}, allowing to handle
	 * multiple exception types.
	 * @see <a href="#splitting">splitting</a>
	 */
	public <E2 extends Exception> Closer<E2> split() {
		return new Closer<>( state );
	}

	/**
	 * @throws E The first throwable caught when executing the {@code push} methods, if any.
	 * Any throwable caught after the first will have been
	 * {@link Throwable#addSuppressed(Throwable) suppressed}.
	 */
	@Override
	public void close() throws E {
		state.close( this );
	}

	@Override
	CloseableState getState() {
		return state;
	}

	@Override
	Closer<E> getSelf() {
		return this;
	}

	static class CloseableState extends State {

		@SuppressWarnings("unchecked")
		<E extends Exception> void close(Closer<E> source) throws E {
			if ( firstThrowable != null && source == firstThrower ) {
				try {
					if ( firstThrowable instanceof RuntimeException ) {
						throw (RuntimeException) firstThrowable;
					}
					else if ( firstThrowable instanceof Error ) {
						throw (Error) firstThrowable;
					}
					else {
						/*
						 * At this point we know that throwable is an instance of E,
						 * because that's the only checked exception that the source
						 * can catch.
						 */
						throw (E) firstThrowable;
					}
				}
				finally {
					// Ensure the next calls to Closer.close won't throw
					this.firstThrower = null;
					this.firstThrowable = null;
				}
			}
		}
	}
}
