/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common;

import org.hibernate.search.engine.common.impl.SearchManagerFactoryBuilderImpl;
import org.hibernate.search.engine.mapper.mapping.MappingType;

/**
 * @author Yoann Rodiere
 */
/*
 * TODO find a better name? The name was "SearchFactory" before 6.0,
 * and it wasn't a good one since the class was not, actually, a factory.
 * But now, it actually is a factory... though "SearchManager" may not be a good name.
 * Maybe MappingManager/MappingManagerFactory?
 */
public interface SearchManagerFactory extends AutoCloseable {

	<T extends SearchManager> T createSearchManager(MappingType<T, ?> mapper);

	<B extends SearchManagerBuilder<?>> B withOptions(MappingType<?, B> mapper);

	@Override
	default void close() {
	}

	static SearchManagerFactoryBuilder builder() {
		return new SearchManagerFactoryBuilderImpl();
	}
}
