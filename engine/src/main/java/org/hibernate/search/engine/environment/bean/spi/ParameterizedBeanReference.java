/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;

public final class ParameterizedBeanReference<T> {
	public static <T> ParameterizedBeanReference<T> of(BeanReference<T> reference, Map<String, ?> params) {
		return new ParameterizedBeanReference<>( reference, params );
	}

	private final BeanReference<T> reference;
	private final Map<String, ?> params;

	private ParameterizedBeanReference(BeanReference<T> reference, Map<String, ?> params) {
		this.reference = reference;
		this.params = params;
	}

	public BeanReference<T> reference() {
		return reference;
	}

	public Map<String, ?> params() {
		return params;
	}
}
