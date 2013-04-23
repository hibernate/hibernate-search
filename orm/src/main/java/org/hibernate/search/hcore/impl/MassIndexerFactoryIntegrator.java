/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.hcore.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.search.impl.DefaultMassIndexerFactory;
import org.hibernate.search.spi.MassIndexerFactory;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Registers a {@link MassIndexerFactory} in the {@link org.hibernate.service.ServiceRegistry}.
 * <p>
 * The type of the factory can be specified in the configuration otherwise a defaul one is used.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MassIndexerFactoryIntegrator implements ServiceContributingIntegrator,
		BasicServiceInitiator<MassIndexerFactory> {

	public static final String MASS_INDEXER_FACTORY_CLASSNAME = "hibernate.search.massindexer.factoryclass";

	@Override
	public Class<MassIndexerFactory> getServiceInitiated() {
		return MassIndexerFactory.class;
	}

	@Override
	public MassIndexerFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		String factoryClassName = (String) configurationValues.get( MASS_INDEXER_FACTORY_CLASSNAME );
		MassIndexerFactory factory = createFactory( factoryClassName );
		factory.initialize( properties( configurationValues ) );
		return factory;
	}

	private Properties properties(Map configurationValues) {
		Properties properties = new Properties();
		properties.putAll( configurationValues );
		return properties;
	}

	private MassIndexerFactory createFactory(String factoryClassName) {
		if ( factoryClassName == null ) {
			return new DefaultMassIndexerFactory();
		}
		else {
			return customFactory( factoryClassName );
		}
	}

	private MassIndexerFactory customFactory(String factoryClassName) {
		return ClassLoaderHelper.instanceFromName( MassIndexerFactory.class, factoryClassName, getClass()
				.getClassLoader(), "Mass indexer factory" );
	}

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void prepareServices(ServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( this );
	}

}
