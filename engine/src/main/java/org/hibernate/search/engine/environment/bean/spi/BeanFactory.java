/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanHolder;

public interface BeanFactory<T> {

	/**
	 * @param context A context object providing ways to create more beans, in particular.
	 * @return The created bean, enclosed in a {@link BeanHolder}.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	BeanHolder<T> create(BeanCreationContext context);

}
