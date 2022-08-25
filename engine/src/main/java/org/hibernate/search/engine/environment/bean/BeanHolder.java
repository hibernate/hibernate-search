/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

/**
 * An object holding a bean instance, and allowing to release it.
 *
 * @param <T> The type of the bean instance.
 */
public interface BeanHolder<T> extends AutoCloseable {

	/**
	 * @return The bean instance. Guaranteed to always return the exact same object,
	 * i.e. {@code beanHolder.get() == beanHolder.get()} is always true.
	 */
	T get();

	/**
	 * Release any resource currently held by the {@link BeanHolder}.
	 * <p>
	 * After this method has been called, the result of calling {@link #get()} on the same instance is undefined.
	 * <p>
	 * <strong>Warning</strong>: this method only releases resources that were allocated
	 * by the creator of the bean instance, and of which the bean instance itself may not be aware.
	 * If the bean instance itself (the one returned by {@link #get()}) exposes any {@code close()}
	 * or other release method, they should be called before the {@link BeanHolder} is released.
	 *
	 * @throws RuntimeException If an error occurs while releasing resources.
	 */
	@Override
	void close();

	/**
	 * @param dependencies Dependencies that should be closed eventually.
	 * @return A bean holder that wraps the current bean holder, and ensures the dependencies are also
	 * closed when its {@link #close()} method is called.
	 */
	default BeanHolder<T> withDependencyAutoClosing(BeanHolder<?> ... dependencies) {
		return new DependencyClosingBeanHolder<>( this, Arrays.asList( dependencies ) );
	}

	/**
	 * @param instance The bean instance.
	 * @param <T> The type of the bean instance.
	 * @return A {@link BeanHolder} whose {@link #get()} method returns the given instance,
	 * and whose {@link #close()} method does not do anything.
	 */
	static <T> BeanHolder<T> of(T instance) {
		return new SimpleBeanHolder<>( instance );
	}

	/**
	 * @param instance The bean instance.
	 * @param <T> The type of the bean instance.
	 * @return A {@link BeanHolder} whose {@link #get()} method returns the given instance,
	 * and whose {@link #close()} method calls {@link Closeable#close()} on the given instance.
	 */
	static <T extends Closeable> BeanHolder<T> ofCloseable(T instance) {
		return new CloseableBeanHolder<>( instance );
	}

	/**
	 * @param beanHolders The bean holders.
	 * @param <T> The type of the bean instances.
	 * @return A {@link BeanHolder} whose {@link #get()} method returns a list containing
	 * the instance of each given bean holder, in order,
	 * and whose {@link #close()} method closes every given bean holder.
	 */
	static <T> BeanHolder<List<T>> of(List<? extends BeanHolder<? extends T>> beanHolders) {
		return new CompositeBeanHolder<>( beanHolders );
	}

}
