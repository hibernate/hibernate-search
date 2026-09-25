/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.accessor.AccessorFactory;

public record AccessorFactoriesContext(
										AccessorFactory accessorFactory,
										AccessorFactory annotationAccessorFactory) {
}
