/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class HibernateOrmRawTypeIdentifierResolver {

	static <T> PojoRawTypeIdentifier<T> createClassTypeIdentifier(Class<T> javaClass) {
		return PojoRawTypeIdentifier.of( javaClass );
	}

	static PojoRawTypeIdentifier<Map> createDynamicMapTypeIdentifier(String name) {
		return PojoRawTypeIdentifier.of( Map.class, name );
	}

	private final Map<Class<?>, PojoRawTypeIdentifier<?>> byJavaClass;
	private final Map<String, PojoRawTypeIdentifier<?>> byHibernateOrmEntityName;

	private HibernateOrmRawTypeIdentifierResolver(Builder builder) {
		this.byJavaClass = Collections.unmodifiableMap( builder.byJavaClass );
		this.byHibernateOrmEntityName = Collections.unmodifiableMap( builder.byHibernateOrmEntityName );
	}

	@SuppressWarnings("unchecked")
	public <T> PojoRawTypeIdentifier<T> resolveByJavaClass(Class<T> javaClass) {
		PojoRawTypeIdentifier<T> result = (PojoRawTypeIdentifier<T>) byJavaClass.get( javaClass );
		if ( result != null ) {
			return result;
		}
		// Non-entity class
		return HibernateOrmRawTypeIdentifierResolver.createClassTypeIdentifier( javaClass );
	}

	public PojoRawTypeIdentifier<?> resolveByHibernateOrmEntityName(String entityName) {
		return byHibernateOrmEntityName.get( entityName );
	}

	public Set<String> getKnownHibernateOrmEntityNames() {
		return byHibernateOrmEntityName.keySet();
	}

	static class Builder {
		private final Map<Class<?>, PojoRawTypeIdentifier<?>> byJavaClass = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeIdentifier<?>> byHibernateOrmEntityName = new LinkedHashMap<>();

		<T> void addClassEntityType(Class<T> javaClass, String hibernateOrmEntityName) {
			PojoRawTypeIdentifier<T> typeIdentifier = createClassTypeIdentifier( javaClass );
			byJavaClass.put( javaClass, typeIdentifier );
			byHibernateOrmEntityName.put( hibernateOrmEntityName, typeIdentifier );
		}

		void addDynamicMapEntityType(String hibernateOrmEntityName) {
			PojoRawTypeIdentifier<Map> typeIdentifier = createDynamicMapTypeIdentifier( hibernateOrmEntityName );
			byHibernateOrmEntityName.put( hibernateOrmEntityName, typeIdentifier );
		}

		HibernateOrmRawTypeIdentifierResolver build() {
			return new HibernateOrmRawTypeIdentifierResolver( this );
		}
	}
}
