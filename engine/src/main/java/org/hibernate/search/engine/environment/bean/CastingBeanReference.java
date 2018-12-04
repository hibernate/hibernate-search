/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

final class CastingBeanReference<T> implements BeanReference<T> {
	private final BeanReference<?> casted;
	private final Class<T> expectedType;

	CastingBeanReference(BeanReference<?> casted, Class<T> expectedType) {
		this.casted = casted;
		this.expectedType = expectedType;
	}

	@Override
	public T getBean(BeanProvider beanProvider) {
		return expectedType.cast( casted.getBean( beanProvider ) );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <U> BeanReference<? extends U> asSubTypeOf(Class<U> expectedType2) {
		if ( expectedType2.isAssignableFrom( expectedType ) ) {
			return (BeanReference<? extends U>) this;
		}
		else {
			return casted.asSubTypeOf( expectedType2 );
		}
	}
}
