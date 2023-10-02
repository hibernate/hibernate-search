/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;

public final class StandalonePojoLoadingTypeGroup<E> implements LoadingTypeGroup<E> {

	private final PojoRuntimeIntrospector introspector;
	private final Set<? extends PojoLoadingTypeContext<? extends E>> includedTypes;

	public StandalonePojoLoadingTypeGroup(Set<? extends PojoLoadingTypeContext<? extends E>> includedTypes,
			PojoRuntimeIntrospector introspector) {
		this.introspector = introspector;
		this.includedTypes = includedTypes;
	}

	@Override
	public Map<String, Class<? extends E>> includedTypesMap() {
		return includedTypes.stream()
				.collect( Collectors.toMap( PojoLoadingTypeContext::entityName,
						t -> t.typeIdentifier().javaClass(), (o1, o2) -> o1, LinkedHashMap::new
				) );
	}

	@Override
	public boolean includesInstance(Object entity) {
		PojoRawTypeIdentifier<?> targetType = introspector.detectEntityType( entity );
		if ( targetType == null ) {
			return false;
		}
		for ( PojoLoadingTypeContext<? extends E> includedType : includedTypes ) {
			if ( includedType.typeIdentifier().equals( targetType ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return includedTypes.stream().map( PojoLoadingTypeContext::entityName )
				.collect( Collectors.joining( "," ) );
	}
}
