/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;

public final class StandalonePojoLoadingTypeGroup<E> implements LoadingTypeGroup<E> {

	private final LoadingTypeContextProvider typeContextProvider;
	private final PojoRuntimeIntrospector introspector;
	private final Set<LoadingTypeContext<? extends E>> includedTypes;

	public StandalonePojoLoadingTypeGroup(LoadingTypeContextProvider typeContextProvider,
			Set<PojoRawTypeIdentifier<? extends E>> includedTypeIdentifiers,
			PojoRuntimeIntrospector introspector) {
		this.typeContextProvider = typeContextProvider;
		this.introspector = introspector;
		this.includedTypes = new LinkedHashSet<>();
		for ( PojoRawTypeIdentifier<? extends E> includedTypeIdentifier : includedTypeIdentifiers ) {
			includedTypes.add( typeContextProvider.forExactType( includedTypeIdentifier ) );
		}
	}

	@Override
	public Map<String, Class<? extends E>> includedTypesMap() {
		return includedTypes.stream()
				.collect( Collectors.toMap( LoadingTypeContext::name,
						t -> t.typeIdentifier().javaClass(), (o1, o2) -> o1, LinkedHashMap::new
				) );
	}

	@Override
	public boolean includesInstance(Object entity) {
		PojoRawTypeIdentifier<?> targetType = introspector.detectEntityType( entity );
		LoadingTypeContext<?> typeContextOrNull = typeContextProvider.forExactTypeOrNull( targetType );
		if ( typeContextOrNull == null ) {
			return false;
		}
		return includedTypes.contains( typeContextOrNull );
	}

	@Override
	public String toString() {
		return includedTypes.stream().map( LoadingTypeContext::name )
				.collect( Collectors.joining( "," ) );
	}
}
