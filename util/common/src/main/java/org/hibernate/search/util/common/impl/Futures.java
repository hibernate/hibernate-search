/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


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


	/**
	 * Creates a future handler that will delegate to the given {@link BiFunction}
	 * after having unwrapped the throwable passed as input if it is a {@link CompletionException}.
	 * <p>
	 * This method is meant to be used in conjunction with {@link CompletableFuture#handle(BiFunction)}.
	 *
	 * @param delegate The handler to delegate to
	 * @return The new, delegating handler.
	 */
	public static <T, R> BiFunction<T, Throwable, R> handler(BiFunction<T, Throwable, R> delegate) {
		return (result, throwable) -> {
			if ( throwable instanceof CompletionException ) {
				throwable = throwable.getCause();
			}
			return delegate.apply( result, throwable );
		};
	}

	/**
	 * Creates a future handler that will delegate to the given {@link BiConsumer}
	 * after having unwrapped the throwable passed as input if it is a {@link CompletionException}.
	 * <p>
	 * This method is meant to be used in conjunction with {@link CompletableFuture#whenComplete(BiConsumer)}.
	 *
	 * @param delegate The handler to delegate to
	 * @return The new, delegating handler.
	 */
	public static <T> BiConsumer<T, Throwable> handler(BiConsumer<T, Throwable> delegate) {
		return (result, throwable) -> {
			if ( throwable instanceof CompletionException ) {
				throwable = throwable.getCause();
			}
			delegate.accept( result, throwable );
		};
	}

	/**
	 * Creates a future handler that will copy the state of the handled future
	 * to the given future.
	 * <p>
	 * This method is meant to be used in conjunction with {@link CompletableFuture#whenComplete(BiConsumer)}.
	 *
	 * @param copyFuture The future to copy to
	 * @return the copy handler
	 */
	public static <T> BiConsumer<T, Throwable> copyHandler(CompletableFuture<T> copyFuture) {
		return (result, throwable) -> {
			if ( throwable != null ) {
				copyFuture.completeExceptionally( throwable );
			}
			else {
				copyFuture.complete( result );
			}
		};
	}

	/**
	 * Creates a composition function that will delegate to the given {@link Function}
	 * but will catch any exception during composition to return a future completed exceptionally.
	 * <p>
	 * This method is meant to be used in conjunction with {@link CompletableFuture#thenCompose(Function)}.
	 * It is useful in particular when you want to apply the same error handling to the composition
	 * function and to the resulting future.
	 *
	 * @param delegate The composition function to delegate to.
	 * @return The new, delegating composition function .
	 */
	public static <T, R> Function<T, CompletionStage<R>> safeComposer(Function<? super T, ? extends CompletionStage<R>> delegate) {
		return result -> {
			try {
				return delegate.apply( result );
			}
			catch (Throwable t) {
				CompletableFuture<R> future = new CompletableFuture<>();
				future.completeExceptionally( t );
				return future;
			}
		};
	}

	/**
	 * Compose the given future with another as soon as it's complete,
	 * regardless of errors, and return a completable future that
	 * will take errors of both parent futures into account
	 * (using {@link Throwable#addSuppressed(Throwable)} if need be).
	 *
	 * @param self The future to wait for before launching the next one
	 * @param action the composition consumer
	 * @return A completable future that will be complete once {@code self} finished executing and
	 * {@code action} and its resulting future finished executing.
	 */
	public static <T> CompletableFuture<T> whenCompleteExecute(CompletableFuture<?> self, Supplier<? extends CompletionStage<T>> action) {
		return self.handle( handler( (result, throwable) -> throwable ) )
				.thenCompose( throwable -> {
					CompletionStage<T> stage;
					try {
						stage = action.get();
					}
					catch (Throwable otherThrowable) {
						CompletableFuture<T> future = new CompletableFuture<>();
						future.completeExceptionally( otherThrowable );
						stage = future;
					}
					if ( throwable != null ) {
						return stage.handle( Futures.<Object, T>handler( (ignored, otherThrowable) -> {
							throw wrap( Throwables.combine( throwable, otherThrowable ) );
						} ) );
					}
					else {
						return stage;
					}
				} );
	}

	/**
	 * Call {@link CompletableFuture#join()} and unwrap any {@link CompletionException},
	 * expecting the exception to be a {@link RuntimeException}.
	 * @param future The future to join on.
	 * @param <T> The type of result the future will return.
	 * @return The result returned by the future.
	 * @throws RuntimeException If the future fails.
	 */
	public static <T> T unwrappedExceptionJoin(CompletableFuture<T> future) {
		try {
			return future.join();
		}
		catch (CompletionException e) {
			throw Throwables.toRuntimeException( e.getCause() );
		}
	}

	/**
	 * Call {@link CompletableFuture#get()} and unwrap any {@link java.util.concurrent.ExecutionException},
	 * expecting the exception to be a {@link RuntimeException}.
	 * @param future The future to join on.
	 * @param <T> The type of result the future will return.
	 * @return The result returned by the future.
	 * @throws RuntimeException If the future fails.
	 * @throws InterruptedException If the thread is interrupted.
	 */
	public static <T> T unwrappedExceptionGet(CompletableFuture<T> future) throws InterruptedException {
		try {
			return future.get();
		}
		catch (ExecutionException e) {
			throw Throwables.toRuntimeException( e.getCause() );
		}
	}

	private static RuntimeException wrap(Throwable throwable) {
		if ( throwable instanceof RuntimeException ) {
			return (RuntimeException) throwable;
		}
		else {
			return new CompletionException( throwable );
		}
	}

	public static Throwable getThrowableNow(CompletableFuture<?> future) {
		try {
			future.getNow( null );
			return null;
		}
		catch (CompletionException e) {
			return e.getCause();
		}
		catch (Throwable t) {
			return t;
		}
	}
}
