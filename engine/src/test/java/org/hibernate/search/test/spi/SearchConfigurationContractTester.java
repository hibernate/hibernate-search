/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;

/**
 * If this class compiles we're good. The idea is that implementors
 * of the SearchConfiguration SPI are all currently aware of the
 * methods listed here today. If we add new methods to the SPI,
 * we need to make sure we add appropriate default implementations
 * in the SearchConfigurationBase.
 *
 * If this class needs to be changed to fix compilation issues
 * after updates in the SearchConfiguration, be careful.
 *
 * The current list of methods are SPI compatible with Hibernate
 * Search version 5.0.0.
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
	public Map<Class<? extends Service>, Object> getProvidedServices() {
		return null;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return null;
	}

	//DON'T ADD NEW METHODS HERE - read file header first.

}
