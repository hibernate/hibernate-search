/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.util.common.impl.Contracts;

final class BeanConfigurationContextImpl implements BeanConfigurationContext {

	private final Map<ConfiguredBeanKey<?>, BeanReference<?>> explicitlyConfiguredBeans = new HashMap<>();

	private final Map<Class<?>, List<BeanReference<?>>> roleMap = new HashMap<>();

	@Override
	public <T> void define(Class<T> exposedType, BeanReference<T> reference) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( reference, "reference" );
		explicitlyConfiguredBeans.put( new ConfiguredBeanKey<>( exposedType, null ), reference );
	}

	@Override
	public <T> void define(Class<T> exposedType, String name, BeanReference<T> reference) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( name, "name" );
		Contracts.assertNotNull( reference, "reference" );
		explicitlyConfiguredBeans.put( new ConfiguredBeanKey<>( exposedType, name ), reference );
	}

	@Override
	public <T> void assignRole(Class<T> role, BeanReference<? extends T> reference) {
		Contracts.assertNotNull( role, "role" );
		Contracts.assertNotNull( reference, "reference" );
		roleMap.computeIfAbsent( role, ignored -> new ArrayList<>() )
				.add( reference );
	}

	Map<ConfiguredBeanKey<?>, BeanReference<?>> getConfiguredBeans() {
		return Collections.unmodifiableMap( explicitlyConfiguredBeans );
	}

	Map<Class<?>, List<? extends BeanReference<?>>> getRoleMap() {
		return Collections.unmodifiableMap( roleMap );
	}
}
