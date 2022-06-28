/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading;

import java.util.List;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastoreConnection;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;

// tag::include[]
public class MySelectionLoadingStrategy<E>
		implements SelectionLoadingStrategy<E> {
	private final Class<E> rootEntityType;

	public MySelectionLoadingStrategy(Class<E> rootEntityType) {
		this.rootEntityType = rootEntityType;
	}

	@Override
	public SelectionEntityLoader<E> createEntityLoader(
			LoadingTypeGroup<E> includedTypes, // <1>
			SelectionLoadingOptions options) {
		MyDatastoreConnection connection =
				options.context( MyDatastoreConnection.class ); // <2>
		return new SelectionEntityLoader<E>() {
			@Override
			public List<E> load(List<?> identifiers, Deadline deadline) {
				return connection.loadEntitiesByIdInSameOrder( // <3>
						rootEntityType, identifiers );
			}
		};
	}
}
// end::include[]
