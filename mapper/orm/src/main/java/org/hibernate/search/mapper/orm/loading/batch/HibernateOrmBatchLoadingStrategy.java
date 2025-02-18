/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.batch;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A strategy for mass loading, used in particular during mass indexing.
 *
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
@Incubating
public interface HibernateOrmBatchLoadingStrategy<E, I> {

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy targets the same entity hierarchy
	 * and can be used as a replacement for this one.
	 * {@code false} otherwise or when unsure.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	@Override
	int hashCode();

	/**
	 * @param typeContext A representation of all entity types that will have to be loaded.
	 * @param options Loading options configured by the requester (who requested batch indexing, ...).
	 * @return An entity identifier loader.
	 */
	HibernateOrmBatchIdentifierLoader createIdentifierLoader(HibernateOrmBatchLoadingTypeContext<E> typeContext,
			HibernateOrmBatchIdentifierLoadingOptions options);

	/**
	 * @param typeContext A representation of all entity types that will have to be loaded.
	 * @param sink A sink to which the entity loader will pass loaded entities.
	 * @param options Loading options configured by the requester (who requested mass indexing, ...).
	 * @return An entity loader.
	 */
	HibernateOrmBatchEntityLoader createEntityLoader(HibernateOrmBatchLoadingTypeContext<E> typeContext,
			HibernateOrmBatchEntitySink<E> sink,
			HibernateOrmBatchEntityLoadingOptions options);

}
