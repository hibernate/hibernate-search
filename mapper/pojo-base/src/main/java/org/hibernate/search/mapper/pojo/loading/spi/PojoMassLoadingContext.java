/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

/**
 * Context exposed to {@link PojoMassLoadingStrategy}.
 * <p>
 * Mappers will generally need to cast this type to the mapper-specific subtype.
 * @see PojoMassIdentifierLoadingContext#parent()
 * @see PojoMassEntityLoadingContext#parent()
 * @see PojoMassLoadingStrategy#groupingAllowed(PojoLoadingTypeContext, PojoMassLoadingContext)
 */
public interface PojoMassLoadingContext {

}
