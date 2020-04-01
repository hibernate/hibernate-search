/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.mapper.pojo.bridge.binding.FilterBindingContext;

/**
 * A binder from a POJO type to a filter factory.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a {@link FilterFactory}.
 *
 * @see FilterFactory
 */
public interface FilterBinder {

	/**
	 * Binds a type to filter factory.
	 * <p>
	 * The context passed in parameter provides various information about the type being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code setBridge(...)} methods on the context
	 * to set the bridge.
	 * <p>
	 * @param context A context object providing information about the type being bound,
	 * and expecting a call to one of its {@code setBridge(...)} methods.
	 */
	void bind(FilterBindingContext<? extends FilterFactory> context);

}
