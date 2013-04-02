/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.spi.ServiceProvider;

/**
 * IFF this class compiles we're good. The idea is that implementors
 * of the SearchConfiguration SPI are all currently aware of the
 * methods listed here today. If we add new methods to the SPI,
 * we need to make sure we add appropriate default implementations
 * in the SearchConfigurationBase.
 *
 * If this class needs to be changed to fix compilation issues
 * after updates in the SearchConfiguration, be careful.
 *
 * The current list of methods are SPI compatible with Hibernate
 * Search version 4.1.0.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SearchConfigurationContractTester extends SearchConfigurationBase implements SearchConfiguration {

	//DON'T ADD NEW METHODS HERE - read file header first.

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return null;
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return null;
	}

	@Override
	public String getProperty(String propertyName) {
		return null;
	}

	@Override
	public Properties getProperties() {
		return null;
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return null;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return null;
	}

	@Override
	public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
		return null;
	}

	//DON'T ADD NEW METHODS HERE - read file header first.

}
