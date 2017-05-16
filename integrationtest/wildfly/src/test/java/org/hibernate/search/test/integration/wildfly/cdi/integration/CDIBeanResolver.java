/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.search.hcore.spi.BeanResolver;


/**
 * @author Yoann Rodiere
 */
public class CDIBeanResolver implements BeanResolver {

	private final BeanManager beanManager;

	public CDIBeanResolver(BeanManager beanManager) {
		super();
		this.beanManager = beanManager;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		Set<Bean<?>> beans = beanManager.getBeans( reference );
		if ( beans.isEmpty() ) {
			throw new IllegalArgumentException( "No CDI bean for class " + reference.getName() );
		}
		else if ( beans.size() > 1 ) {
			throw new IllegalArgumentException( "Multiple CDI beans for class " + reference.getName() + ": " + beans );
		}
		else {
			Object beanInstance = getBeanInstance( beans.iterator().next() );
			return expectedClass.cast( beanInstance );
		}
	}

	private <T> T getBeanInstance(Bean<T> bean) {
		CreationalContext<T> context = beanManager.createCreationalContext( null );
		return bean.create( context );
	}

}
