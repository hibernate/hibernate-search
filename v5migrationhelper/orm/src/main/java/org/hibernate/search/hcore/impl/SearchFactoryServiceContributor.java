/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Extends the ServiceRegistry of Hibernate ORM to contain the placeholders needed
 * by Hibernate Search
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public final class SearchFactoryServiceContributor implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addService( SearchFactoryReference.class, new SearchFactoryReference() );
	}

}
