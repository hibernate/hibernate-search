/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Yoann Rodiere
 */
public final class Futures {

	private Futures() {
	}

	/**
	 * Create a {@link CompletableFuture} using the given supplier.
	 * <p>
	 * This method is guaranteed to never throw any exception: any exception thrown by
	 * the given supplier will instead complete the resulting future
	 * {@link CompletableFuture#completeExceptionally(Throwable) exceptionally}.
	 * <p>
	 * This is useful in particular if you want to handle errors during the {@link CompletableFuture}
	 * creation the same way as errors thrown during post-processing operations
	 * (for instance operations passed to {@link CompletableFuture#thenApply(Function)}).
	 *
	 * @param initiator A supplier that will initiate (synchronously) the asynchronous operation.
	 * @return A {@link CompletableFuture} wrapping the result of both the initiation and execution of the operation.
	 */
	public static <T> CompletableFuture<T> create(Supplier<CompletableFuture<T>> initiator) {
		return CompletableFuture.completedFuture( null ).thenCompose( ignored -> initiator.get() );
	}

	/**
	 * Creates a future handler that will delegate to the given {@link Function}
	 * after having unwrapped the throwable passed as input if it is a {@link CompletionException}.
	 * <p>
	 * This method is meant to be used in conjunction with {@link CompletableFuture#exceptionally(Function)}.
	 *
	 * @param delegate The exception function to delegate to
	 * @return The new, delegating exception function.
	 */
	public static <T> Function<Throwable, T> handler(Function<Throwable, T> delegate) {
		return throwable -> {
			if ( throwable instanceof CompletionException ) {
				throwable = throwable.getCause();
			}
			return delegate.apply( throwable );
		};
	}

}
