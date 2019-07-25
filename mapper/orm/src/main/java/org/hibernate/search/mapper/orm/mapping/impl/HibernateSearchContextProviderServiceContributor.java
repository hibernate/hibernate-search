/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
