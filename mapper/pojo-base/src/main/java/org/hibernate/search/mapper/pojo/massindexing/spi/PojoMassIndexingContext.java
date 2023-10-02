/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.Set;

import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;

/**
 * Contextual information about a mass indexing proccess.
 */
public interface PojoMassIndexingContext extends PojoMassLoadingContext {

	Set<String> tenantIds();

	TenancyMode tenancyMode();

	MassIndexingDefaultCleanOperation massIndexingDefaultCleanOperation();
}
