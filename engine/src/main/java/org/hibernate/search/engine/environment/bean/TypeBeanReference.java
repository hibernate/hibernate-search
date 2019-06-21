/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import org.hibernate.search.util.common.impl.Contracts;

class TypeBeanReference<T> implements BeanReference<T> {

	final Class<T> type;

	TypeBeanReference(Class<T> type) {
		Contracts.assertNotNull( type, "type" );
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[type=" + type + "]";
	}

	@Override
	public BeanHolder<T> resolve(BeanResolver beanResolver) {
		return beanResolver.resolve( type );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <U> BeanReference<? extends U> asSubTypeOf(Class<U> expectedType) {
		if ( expectedType.isAssignableFrom( type ) ) {
			return (BeanReference<? extends U>) this;
		}
		else {
			return BeanReference.super.asSubTypeOf( expectedType );
		}
	}

}
