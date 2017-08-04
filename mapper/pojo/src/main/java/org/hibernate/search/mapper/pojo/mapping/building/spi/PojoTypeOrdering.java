/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;


/**
 * @author Yoann Rodiere
 */
public final class PojoTypeOrdering implements IndexableTypeOrdering {

	private static final PojoTypeOrdering INSTANCE = new PojoTypeOrdering();

	public static IndexableTypeOrdering get() {
		return INSTANCE;
	}

	private PojoTypeOrdering() {
	}

	@Override
	public boolean isSubType(IndexedTypeIdentifier parent, IndexedTypeIdentifier subType) {
		return toClass( parent ).isAssignableFrom( toClass( subType ) );
	}

	@Override
	public Collection<IndexedTypeIdentifier> getAscendingSuperTypes(IndexedTypeIdentifier subType) {
		/*
		 * Interfaces can be implemented multiple times,
		 * so we use a LinkedHashSet to preserve order while removing duplicates.
		 */
		Set<IndexedTypeIdentifier> result = new LinkedHashSet<>();
		collectSuperTypesAscending( result, toClass( subType ) );
		return result;
	}

	@Override
	public Collection<IndexedTypeIdentifier> getDescendingSuperTypes(IndexedTypeIdentifier subType) {
		/*
		 * Interfaces can be implemented multiple times,
		 * so we use a LinkedHashSet to preserve order while removing duplicates.
		 */
		Set<IndexedTypeIdentifier> result = new LinkedHashSet<>();
		collectSuperTypesDescending( result, toClass( subType ) );
		return result;
	}

	private void collectSuperTypesAscending(Collection<IndexedTypeIdentifier> result, Class<?> subType) {
		result.add( toId( subType ) );
		for ( Class<?> interfaze : subType.getInterfaces() ) {
			collectSuperTypesAscending( result, interfaze );
		}
		if ( !Object.class.equals( subType ) ) {
			collectSuperTypesAscending( result, subType.getSuperclass() );
		}
	}

	private void collectSuperTypesDescending(Collection<IndexedTypeIdentifier> result, Class<?> subType) {
		if ( !Object.class.equals( subType ) ) {
			collectSuperTypesDescending( result, subType.getSuperclass() );
		}
		for ( Class<?> interfaze : subType.getInterfaces() ) {
			collectSuperTypesDescending( result, interfaze );
		}
		result.add( toId( subType ) );
	}

	private Class<?> toClass(IndexedTypeIdentifier typeId) {
		return ( (PojoIndexedTypeIdentifier) typeId ).toJavaType();
	}

	private PojoIndexedTypeIdentifier toId(Class<?> clazz) {
		return new PojoIndexedTypeIdentifier( clazz );
	}

}
