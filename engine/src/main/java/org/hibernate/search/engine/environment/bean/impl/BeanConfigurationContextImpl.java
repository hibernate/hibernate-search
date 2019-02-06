/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanFactory;
import org.hibernate.search.util.impl.common.Contracts;

final class BeanConfigurationContextImpl implements BeanConfigurationContext {

	private final Map<ConfiguredBeanKey<?>, BeanFactory<?>> explicitlyConfiguredBeans = new HashMap<>();

	@Override
	public <T> void define(Class<T> exposedType, BeanFactory<T> factory) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( factory, "factory" );
		explicitlyConfiguredBeans.put( new ConfiguredBeanKey<>( exposedType, null ), factory );
	}

	@Override
	public <T> void define(Class<T> exposedType, String name, BeanFactory<T> factory) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( name, "name" );
		Contracts.assertNotNull( factory, "factory" );
		explicitlyConfiguredBeans.put( new ConfiguredBeanKey<>( exposedType, name ), factory );
	}

	Map<ConfiguredBeanKey<?>, BeanFactory<?>> getConfiguredBeans() {
		return Collections.unmodifiableMap( explicitlyConfiguredBeans );
	}

}
