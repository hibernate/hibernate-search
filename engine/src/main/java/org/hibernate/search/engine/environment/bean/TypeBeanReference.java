/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

final class TypeBeanReference implements BeanReference {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Class<?> type;

	TypeBeanReference(Class<?> type) {
		if ( type == null ) {
			throw log.invalidBeanReferenceNameNullOrEmpty();
		}
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[type=" + type + "]";
	}

	@Override
	public <T> T getBean(BeanProvider beanProvider, Class<T> expectedType) {
		return beanProvider.getBean( expectedType, type );
	}

}
