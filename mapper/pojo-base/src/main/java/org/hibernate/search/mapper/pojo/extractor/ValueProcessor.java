/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor;

/**
 * A processor of values extracted from a container.
 *
 * @param <T> The type of the {@code target} of this processor,
 * i.e. whatever it is supposed to push the result of its processing to.
 * @param <V> The type of values processed by this processor.
 * @param <C> The type of the {@code context} of this processor,
 * i.e. whatever information it needs that is independent from the target or value.
 * @see ContainerExtractor#extract(Object, ValueProcessor, Object, Object)
 */
public interface ValueProcessor<T, V, C> {

	/**
	 * @param target The {@code target} passed to
	 * {@link ContainerExtractor#extract(Object, ValueProcessor, Object, Object)}.
	 * @param value The value to process.
	 * @param context The {@code context} passed to
	 * {@link ContainerExtractor#extract(Object, ValueProcessor, Object, Object)}.
	 */
	void process(T target, V value, C context);

}
