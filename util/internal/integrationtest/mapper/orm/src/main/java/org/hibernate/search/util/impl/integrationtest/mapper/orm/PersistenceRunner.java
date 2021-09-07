/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * An easy way to run code in the context of an {@link EntityManager}/{@link org.hibernate.Session}
 * and transaction (both instantiated by the runner).
 *
 * @param <C> The type of persistence context: {@link EntityManager} or {@link org.hibernate.Session}.
 * @param <T> The type of transaction: {@link EntityTransaction} or {@link org.hibernate.Transaction}.
 */
public interface PersistenceRunner<C, T> {

	<R> R applyNoTransaction(Function<? super C, R> action);

	default void runNoTransaction(Consumer<? super C> action) {
		applyNoTransaction( c -> {
			action.accept( c );
			return null;
		} );
	}

	default <R> R applyInTransaction(Function<? super C, R> action) {
		return applyInTransaction( (c, t) -> action.apply( c ) );
	}

	<R> R applyInTransaction(BiFunction<? super C, ? super T, R> action);

	default void runInTransaction(Consumer<? super C> action) {
		applyInTransaction( c -> {
			action.accept( c );
			return null;
		} );
	}

	default void runInTransaction(BiConsumer<? super C, T> action) {
		applyInTransaction( (c, t) -> {
			action.accept( c, t );
			return null;
		} );
	}

}
