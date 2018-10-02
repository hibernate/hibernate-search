/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.service.impl;

import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.service.spi.ServiceManager;


/**
 * @author Yoann Rodiere
 */
public class ServiceManagerImpl implements ServiceManager {

	private final ClassResolver classResolver;
	private final ResourceResolver resourceResolver;
	private final BeanProvider beanProvider;

	public ServiceManagerImpl(ClassResolver classResolver, ResourceResolver resourceResolver,
			BeanProvider beanProvider) {
		this.classResolver = classResolver;
		this.resourceResolver = resourceResolver;
		this.beanProvider = beanProvider;
	}

	@Override
	public ClassResolver getClassResolver() {
		return classResolver;
	}

	@Override
	public ResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	@Override
	public BeanProvider getBeanProvider() {
		return beanProvider;
	}

}
