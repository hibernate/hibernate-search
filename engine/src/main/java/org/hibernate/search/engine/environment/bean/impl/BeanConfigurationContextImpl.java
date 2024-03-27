/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.util.common.impl.Contracts;

final class BeanConfigurationContextImpl implements BeanConfigurationContext {

	private final Map<Class<?>, BeanReferenceRegistryForType<?>> configuredBeans = new HashMap<>();

	@Override
	public <T> void define(Class<T> exposedType, BeanReference<? extends T> reference) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( reference, "reference" );
		configuredBeans( exposedType ).add( reference );
	}

	@Override
	public <T> void define(Class<T> exposedType, String name, BeanReference<? extends T> reference) {
		Contracts.assertNotNull( exposedType, "exposedType" );
		Contracts.assertNotNull( name, "name" );
		Contracts.assertNotNull( reference, "reference" );
		configuredBeans( exposedType ).add( name, reference );
	}

	ConfigurationBeanRegistry buildRegistry() {
		return new ConfigurationBeanRegistry( new HashMap<>( configuredBeans ) );
	}

	@SuppressWarnings("unchecked")
	private <T> BeanReferenceRegistryForType<T> configuredBeans(Class<T> exposedType) {
		return (BeanReferenceRegistryForType<T>) configuredBeans.computeIfAbsent( exposedType,
				ignored -> new BeanReferenceRegistryForType<>( exposedType ) );
	}
}
