/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.logging.impl.EngineMiscLog;

final class ConfigurationBeanRegistry {

	private final Map<Class<?>, BeanReferenceRegistryForType<?>> explicitlyConfiguredBeans;

	ConfigurationBeanRegistry(Map<Class<?>, BeanReferenceRegistryForType<?>> explicitlyConfiguredBeans) {
		this.explicitlyConfiguredBeans = explicitlyConfiguredBeans;
	}

	public <T> BeanHolder<T> resolve(Class<T> typeReference, BeanResolver beanResolver) {
		BeanReferenceRegistryForType<T> registry = explicitlyConfiguredBeans( typeReference );
		BeanReference<T> reference = null;
		if ( registry != null ) {
			reference = registry.single();
		}
		if ( reference != null ) {
			return beanResolver.resolve( reference );
		}
		else {
			throw EngineMiscLog.INSTANCE.noConfiguredBeanReferenceForType( typeReference );
		}
	}

	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference,
			BeanResolver beanResolver) {
		BeanReferenceRegistryForType<T> registry = explicitlyConfiguredBeans( typeReference );
		BeanReference<T> reference = null;
		if ( registry != null ) {
			reference = registry.named( nameReference );
		}
		if ( reference != null ) {
			return beanResolver.resolve( reference );
		}
		else {
			throw EngineMiscLog.INSTANCE.noConfiguredBeanReferenceForTypeAndName( typeReference, nameReference );
		}
	}

	@SuppressWarnings("unchecked") // We know the registry has the correct type, see BeanConfigurationContextImpl
	public <T> BeanReferenceRegistryForType<T> explicitlyConfiguredBeans(Class<T> exposedType) {
		return (BeanReferenceRegistryForType<T>) explicitlyConfiguredBeans.get( exposedType );
	}
}
