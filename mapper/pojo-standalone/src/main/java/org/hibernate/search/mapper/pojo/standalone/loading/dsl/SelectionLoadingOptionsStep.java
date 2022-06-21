/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.dsl;

import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The DSL entry point passed to consumers in
 * {@link org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder#loading(Consumer)},
 * allowing the definition of context for use by selection loading strategies:
 * connections, third-party sessions, ...
 */
@Incubating
public interface SelectionLoadingOptionsStep {

	/**
	 * Sets context for use by selection loading strategies: connections, third-party sessions, ...
	 * <p>
	 * The context can be retrieved through
	 * {@link SelectionLoadingOptions#context(Class)}.
	 *
	 * @param <T> The type of context.
	 * @param contextType The type of context, used as a key to retrieve it from
	 * {@link SelectionLoadingOptions#context(Class)}.
	 * @param context The context instance.
	 */
	<T> void context(Class<T> contextType, T context);

}
