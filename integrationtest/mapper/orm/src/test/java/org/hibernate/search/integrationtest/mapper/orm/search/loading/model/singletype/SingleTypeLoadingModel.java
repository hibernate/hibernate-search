/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.Arrays;
import java.util.List;

public abstract class SingleTypeLoadingModel<T> {

	public static List<SingleTypeLoadingModel<?>> all() {
		return Arrays.asList(
				new BasicModel()
				// TODO add more
		);
	}

	@Override
	public final String toString() {
		return describe();
	}

	protected abstract String describe();

	public abstract String getIndexName();

	public abstract Class<T> getIndexedClass();

	public abstract Class<?> getContainedClass();

	public abstract String getIndexedEntityName();

	public abstract String getEagerGraphName();

	public abstract String getLazyGraphName();

	public abstract T newIndexed(int id, SingleTypeLoadingMapping mapping);

	public abstract T newIndexedWithContained(int id, SingleTypeLoadingMapping mapping);

	public abstract Object getContainedEager(T entity);

	public abstract List<?> getContainedLazy(T entity);

}
