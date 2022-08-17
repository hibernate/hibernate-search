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

	@SuppressWarnings("rawtypes")
	static PojoRawTypeIdentifier<Map> createDynamicMapTypeIdentifier(String name) {
		return PojoRawTypeIdentifier.of( Map.class, name );
	}

	private final Map<Class<?>, PojoRawTypeIdentifier<?>> byJavaClass;
	private final Map<String, PojoRawTypeIdentifier<?>> byHibernateOrmEntityName;
	private final Map<String, PojoRawTypeIdentifier<?>> byJpaOrHibernateOrmEntityName;

	private HibernateOrmRawTypeIdentifierResolver(Builder builder) {
		this.byJavaClass = Collections.unmodifiableMap( builder.byJavaClass );
		this.byHibernateOrmEntityName = Collections.unmodifiableMap( builder.byHibernateOrmEntityName );
		this.byJpaOrHibernateOrmEntityName = Collections.unmodifiableMap( builder.byJpaOrHibernateOrmEntityName );
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

	public PojoRawTypeIdentifier<?> resolveByJpaOrHibernateOrmEntityName(String entityName) {
		return byJpaOrHibernateOrmEntityName.get( entityName );
	}

	public Set<String> allKnownHibernateOrmEntityNames() {
		return byHibernateOrmEntityName.keySet();
	}

	public Set<String> allKnownJpaOrHibernateOrmEntityNames() {
		return byJpaOrHibernateOrmEntityName.keySet();
	}

	static class Builder {
		private final Map<Class<?>, PojoRawTypeIdentifier<?>> byJavaClass = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeIdentifier<?>> byJpaEntityName = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeIdentifier<?>> byHibernateOrmEntityName = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeIdentifier<?>> byJpaOrHibernateOrmEntityName = new LinkedHashMap<>();

		<T> void addClassEntityType(Class<T> javaClass, String jpaEntityName, String hibernateOrmEntityName) {
			PojoRawTypeIdentifier<T> typeIdentifier = createClassTypeIdentifier( javaClass );
			byJavaClass.put( javaClass, typeIdentifier );
			addByName( typeIdentifier, jpaEntityName, hibernateOrmEntityName );
		}

		@SuppressWarnings("rawtypes")
		void addDynamicMapEntityType(String jpaEntityName, String hibernateOrmEntityName) {
			PojoRawTypeIdentifier<Map> typeIdentifier = createDynamicMapTypeIdentifier( hibernateOrmEntityName );
			addByName( typeIdentifier, jpaEntityName, hibernateOrmEntityName );
		}

		/*
		 * There are two names for each entity type: the JPA name and the Hibernate ORM name.
		 * They are often different:
		 *  - the Hibernate ORM name is the fully-qualified class name for class entities,
		 *    or the name defined in the hbm.xml for dynamic-map entities.
		 *  - by default, the JPA name is the unqualified class name by default for class entities,
		 *    or the name defined in the hbm.xml for dynamic-map entities.
		 *    It can be overridden with @Entity(name = ...) for class entities.
		 *
		 * In theory, there could be conflicts where a given name points to one entity for JPA
		 * and another entity for ORM. However that would require a very strange mapping:
		 * one would need to set the JPA name of one entity to the fully qualified name of another entity class.
		 *
		 * In Hibernate Search APIs, we accept that conflicts can arise:
		 * JPA entity names will always work,
		 * and when there is no naming conflict (99% of the time) Hibernate ORM entity names will work too.
		 *
		 * We still keep around the map by Hibernate ORM entity name because Search often needs to use
		 * the Hibernate ORM name internally when using Hibernate ORM features (Session.load, ...).
		 */
		private <T> void addByName(PojoRawTypeIdentifier<T> typeIdentifier, String jpaEntityName,
				String hibernateOrmEntityName) {
			byJpaEntityName.put( jpaEntityName, typeIdentifier );
			byHibernateOrmEntityName.put( hibernateOrmEntityName, typeIdentifier );

			byJpaOrHibernateOrmEntityName.put( jpaEntityName, typeIdentifier );
			// Use putIfAbsent here to avoid overriding JPA entity names, see above.
			byJpaOrHibernateOrmEntityName.putIfAbsent( hibernateOrmEntityName, typeIdentifier );
		}

		HibernateOrmRawTypeIdentifierResolver build() {
			return new HibernateOrmRawTypeIdentifierResolver( this );
		}
	}
}
