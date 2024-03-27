/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.Arrays;
import java.util.List;

public abstract class SingleTypeLoadingModel<T> {

	public static List<SingleTypeLoadingModel<?>> all() {
		return Arrays.asList(
				new BasicModel(),
				new FetchSubSelectModel()
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

	public abstract void clearContainedEager(T entity);

	public abstract List<?> getContainedLazy(T entity);

}
