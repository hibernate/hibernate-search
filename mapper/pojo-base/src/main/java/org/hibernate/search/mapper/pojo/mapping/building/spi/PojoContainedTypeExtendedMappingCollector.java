/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

/**
 * A collector of extended mapping information.
 * <p>
 * This should be implemented by POJO mapper implementors in order to collect metadata
 * necessary to implement their {@link org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider}.
 */
public interface PojoContainedTypeExtendedMappingCollector extends PojoTypeExtendedMappingCollector {

}
