/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.spi;

import java.util.function.BiConsumer;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.search.mapper.orm.bootstrap.impl.HibernateOrmIntegrationBooterImpl;
import org.hibernate.service.ServiceRegistry;

public interface HibernateOrmIntegrationBooter {

	static HibernateOrmIntegrationBooter.Builder builder(Metadata metadata, ServiceRegistry serviceRegistry,
			ClassDetailsRegistry classDetailsRegistry) {
		return new HibernateOrmIntegrationBooterImpl.BuilderImpl( metadata, serviceRegistry, classDetailsRegistry );
	}

	interface Builder {
		Builder accessorFactory(AccessorFactory accessorFactory);

		Builder annotationAccessorFactory(AccessorFactory annotationAccessorFactory);

		HibernateOrmIntegrationBooter build();
	}

	void preBoot(BiConsumer<String, Object> propertyCollector);

}
