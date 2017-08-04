/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.common.SearchManagerBuilder;

/**
 * @author Yoann Rodiere
 */
public interface Mapping<B extends SearchManagerBuilder<?>> extends AutoCloseable {

	B createManagerBuilder();

	@Override
	default void close() {
	}

}
