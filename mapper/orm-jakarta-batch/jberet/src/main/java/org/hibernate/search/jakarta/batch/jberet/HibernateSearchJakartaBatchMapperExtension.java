/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.jberet;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Hibernate Search Jakarta Batch JBeret is an extension to the `hibernate-search-mapper-orm-jakarta-batch-core`
 * that is aware of JBeret-specific things.
 *
 * @see org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob
 */
@Incubating
@Deprecated(since = "8.0", forRemoval = true) // DO NOT actually remove this one! We need it to let the javadoc plugin to generate the "empty" javadoc jar to comply with the publishign rules.
public interface HibernateSearchJakartaBatchMapperExtension {
}
