/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.ObjectLoader;

/**
 * An {@link ObjectLoader} that can be easily composed with others object loaders.
 * <p>
 * See {@link ByTypeObjectLoader} for uses.
 * @param <R>
 * @param <O>
 */
interface ComposableObjectLoader<R, O> extends ObjectLoader<R, O> {

	/**
	 * For each reference in the given list,
	 * loads the corresponding object and puts it as a value in the given map.
	 * <p>
	 * When an object cannot be found, the map is not altered.
	 *
	 * @param references A list of references to the objects to load.
	 * @param objectsByReference A map with references as keys and objects as values.
	 * Initial values are undefined and the loader must not rely on them.
	 */
	void load(List<R> references, Map<? super R, ? super O> objectsByReference);

}
