/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A registry for beans of a given exposed type that were explicitly registered through a
 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurer}.
 *
 * @param <T> The type exposed by beans in this registry.
 */
public class BeanReferenceRegistryForType<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Class<T> exposedType;
	private final List<BeanReference<T>> all = new ArrayList<>();
	private final Map<String, BeanReference<T>> named = new TreeMap<>();

	public BeanReferenceRegistryForType(Class<T> exposedType) {
		this.exposedType = exposedType;
	}

	public final List<BeanReference<T>> all() {
		return Collections.unmodifiableList( all );
	}

	public Map<String, BeanReference<T>> named() {
		return Collections.unmodifiableMap( named );
	}

	public BeanReference<T> single() {
		if ( all.size() == 1 ) {
			return all.get( 0 );
		}
		else if ( all.size() > 1 ) {
			throw log.multipleConfiguredBeanReferencesForType( exposedType, all );
		}
		else {
			return null;
		}
	}

	public BeanReference<T> named(String name) {
		return named.get( name );
	}

	@SuppressWarnings("unchecked") // Safe cast from BeanReference<? extends T> to BeanReference<T> as BeanReference is covariant in T
	void add(BeanReference<? extends T> reference) {
		all.add( (BeanReference<T>) reference );
	}

	@SuppressWarnings("unchecked") // Safe cast from BeanReference<? extends T> to BeanReference<T> as BeanReference is covariant in T
	void add(String name, BeanReference<? extends T> reference) {
		Object previous = named.putIfAbsent( name, (BeanReference<T>) reference );
		if ( previous != null ) {
			throw new AssertionFailure( String.format( Locale.ROOT,
					"Duplicate bean references for name '%1$s': %2$s, %3$s",
					name, previous, reference ) );
		}
		all.add( (BeanReference<T>) reference );
	}
}
