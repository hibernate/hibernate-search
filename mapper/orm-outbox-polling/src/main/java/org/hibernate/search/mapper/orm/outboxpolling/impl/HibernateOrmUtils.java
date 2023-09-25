/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
