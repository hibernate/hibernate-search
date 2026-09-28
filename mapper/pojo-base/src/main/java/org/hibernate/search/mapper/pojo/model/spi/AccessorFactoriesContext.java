package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.accessor.AccessorFactory;

public record AccessorFactoriesContext(
										AccessorFactory accessorFactory,
										AccessorFactory annotationAccessorFactory) {
}
