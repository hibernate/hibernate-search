/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.hibernate.search.util.impl.test.function.ThrowingBiConsumer;
import org.hibernate.search.util.impl.test.function.ThrowingBiFunction;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

/**
 * An easy way to run code in the context of an {@link EntityManager}/{@link org.hibernate.Session}
 * and transaction (both instantiated by the runner).
 *
 * @param <C> The type of persistence context: {@link EntityManager} or {@link org.hibernate.Session}.
 * @param <T> The type of transaction: {@link EntityTransaction} or {@link org.hibernate.Transaction}.
 */
public interface PersistenceRunner<C, T> {

	<R, E extends Throwable> R applyNoTransaction(ThrowingFunction<? super C, R, E> action) throws E;

	default <E extends Throwable> void runNoTransaction(ThrowingConsumer<? super C, E> action) throws E {
		applyNoTransaction( c -> {
			action.accept( c );
			return null;
		} );
	}

	default <R, E extends Throwable> R applyInTransaction(ThrowingFunction<? super C, R, E> action) throws E {
		return applyInTransaction( (c, t) -> action.apply( c ) );
	}

	<R, E extends Throwable> R applyInTransaction(ThrowingBiFunction<? super C, ? super T, R, E> action) throws E;

	default <E extends Throwable> void runInTransaction(ThrowingConsumer<? super C, E> action) throws E {
		applyInTransaction( c -> {
			action.accept( c );
			return null;
		} );
	}

	default <E extends Throwable> void runInTransaction(ThrowingBiConsumer<? super C, T, E> action) throws E {
		applyInTransaction( (c, t) -> {
			action.accept( c, t );
			return null;
		} );
	}

}
