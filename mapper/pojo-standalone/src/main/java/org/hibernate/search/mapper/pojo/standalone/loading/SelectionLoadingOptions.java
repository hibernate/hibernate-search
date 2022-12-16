/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface SelectionLoadingOptions {

	/**
	 * Gets context previously passed to
	 * {@link SelectionLoadingOptionsStep#context(Class, Object)}.
	 *
	 * @param <T> The context type.
	 * @param contextType The context type.
	 * @return The context, i.e. an instance of the given type, or {@code null} if no context was set for this type.
	 */
	<T> T context(Class<T> contextType);

}
