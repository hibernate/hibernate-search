/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.batch;

import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface HibernateOrmBatchIdentifierLoadingOptions {

	int fetchSize();

	OptionalInt maxResults();

	OptionalInt offset();

	Optional<HibernateOrmBatchReindexCondition> reindexOnlyCondition();

	Optional<Object> upperBound();

	boolean upperBoundInclusive();

	Optional<Object> lowerBound();

	boolean lowerBoundInclusive();

	/**
	 * Search will add a {@link org.hibernate.StatelessSession} to the context by default.
	 */
	<T> T context(Class<T> contextType);
}
