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

	default <R> R apply(Function<? super C, R> action) {
		return apply( (c, t) -> action.apply( c ) );
	}

	<R> R apply(BiFunction<? super C, ? super T, R> action);

	default void run(Consumer<? super C> action) {
		apply( c -> {
			action.accept( c );
			return null;
		} );
	}

	default void run(BiConsumer<? super C, T> action) {
		apply( (c, t) -> {
			action.accept( c, t );
			return null;
		} );
	}

}
