/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.service.impl;

import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.environment.service.spi.ServiceManager;


/**
 * @author Yoann Rodiere
 */
public class ServiceManagerImpl implements ServiceManager {

	private final ClassLoaderService classLoaderService;
	private final BeanProvider beanProvider;

	public ServiceManagerImpl(ClassLoaderService classLoaderService, BeanProvider beanProvider) {
		this.classLoaderService = classLoaderService;
		this.beanProvider = beanProvider;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public BeanProvider getBeanProvider() {
		return beanProvider;
	}

}
