/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.integration;

import org.hibernate.search.hcore.spi.BeanResolver;
import org.springframework.beans.factory.BeanFactory;


/**
 * @author Yoann Rodiere
 */
public class SpringBeanResolver implements BeanResolver {

	private final BeanFactory beanFactory;

	public SpringBeanResolver(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		if ( reference.isAnnotationPresent( ResolveInHibernateSearch.class ) ) {
			return expectedClass.cast( beanFactory.getBean( reference ) );
		}
		else {
			return null;
		}
	}

}
