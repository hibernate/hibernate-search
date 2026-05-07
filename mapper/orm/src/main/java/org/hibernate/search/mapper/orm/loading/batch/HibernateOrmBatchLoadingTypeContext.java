/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.batch;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface HibernateOrmBatchLoadingTypeContext<E> {

	Class<E> javaClass();

	String jpaEntityName();

	String uniquePropertyName();

}
