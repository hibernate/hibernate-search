/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.impl;

import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;

public final class HibernateOrmUtils {

	private HibernateOrmUtils() {
	}

	public static boolean isDiscriminatorMultiTenancyEnabled(MetadataBuildingContext buildingContext) {
		return buildingContext.getMetadataCollector().getFilterDefinition( TenantIdBinder.FILTER_NAME ) != null;
	}
}
