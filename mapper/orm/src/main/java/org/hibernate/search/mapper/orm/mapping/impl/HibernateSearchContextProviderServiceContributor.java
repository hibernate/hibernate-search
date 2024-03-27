/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Contributes the {@link HibernateSearchContextProviderService} to the ServiceRegistry of Hibernate ORM.
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public final class HibernateSearchContextProviderServiceContributor implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		HibernateSearchContextProviderService service = new HibernateSearchContextProviderService();
		// For initialization
		serviceRegistryBuilder.addService( HibernateSearchContextProviderService.class, service );
	}

}
