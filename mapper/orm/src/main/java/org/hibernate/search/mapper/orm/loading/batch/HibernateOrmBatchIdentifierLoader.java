/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.batch;

import java.util.OptionalLong;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * TODO
 */
@Incubating
public interface HibernateOrmBatchIdentifierLoader extends AutoCloseable {

	/**
	 * Closes this {@link HibernateOrmBatchIdentifierLoader}.
	 */
	@Override
	void close();

	/**
	 * @return The total count of identifiers expected to be loaded.
	 */
	OptionalLong totalCount();

	/**
	 * Loads the next identifier
	 */
	Object next();

	boolean hasNext();

}
