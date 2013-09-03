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

import java.util.Properties;

import org.hibernate.search.impl.DefaultMassIndexerFactory;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.MassIndexerFactory;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * Registers a {@link MassIndexerFactory} as a registered Service.
 * <p>
 * The type of the factory can be specified in the configuration otherwise a default one is used.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class MassIndexerFactoryProvider implements ServiceProvider<MassIndexerFactory> {

	public static final String MASS_INDEXER_FACTORY_CLASSNAME = "hibernate.search.massindexer.factoryclass";

	private volatile MassIndexerFactory massIndexerFactory;

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
	public void start(Properties properties, BuildContext context) {
		String factoryClassName = properties.getProperty( MASS_INDEXER_FACTORY_CLASSNAME );
		massIndexerFactory = createFactory( factoryClassName );
		massIndexerFactory.initialize( properties );
	}

	@Override
	public MassIndexerFactory getService() {
		return massIndexerFactory;
	}

	@Override
	public void stop() {
		// no-op
	}

}
