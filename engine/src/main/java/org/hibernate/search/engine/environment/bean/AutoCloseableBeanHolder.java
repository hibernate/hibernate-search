/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A bean holder that calls {@link AutoCloseable#close()} on its instance upon being {@link #close() closed}.
 * @param <T>
 */
final class AutoCloseableBeanHolder<T extends AutoCloseable> implements BeanHolder<T> {

	private final T instance;

	AutoCloseableBeanHolder(T instance) {
		this.instance = instance;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "instance=" + instance
				+ "]";
	}

	@Override
	public T get() {
		return instance;
	}

	@Override
	public void close() {
		try {
			instance.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e.getMessage(), e );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}
