/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.SyntheticPojoGenericTypeModel;

public interface HibernateOrmGenericTypeModelFactory<T> {

	PojoGenericTypeModel<T> create(HibernateOrmBootstrapIntrospector introspector);

	@SuppressWarnings("unchecked")
	static <T> HibernateOrmGenericTypeModelFactory<T> entityReference(Class<T> javaClass, String entityName) {
		if ( Map.class.equals( javaClass ) ) {
			return (HibernateOrmGenericTypeModelFactory<T>) dynamicMap( entityName );
		}
		else {
			return rawType( javaClass );
		}
	}

	static <T> HibernateOrmGenericTypeModelFactory<T> rawType(Class<T> javaClass) {
		return introspector -> introspector.genericTypeModel( javaClass );
	}

	// This cast is safe if the caller made sure that this name really points to a dynamic-map type
	@SuppressWarnings("unchecked")
	static HibernateOrmGenericTypeModelFactory<Map> dynamicMap(String name) {
		return introspector -> SyntheticPojoGenericTypeModel.opaqueType(
				(PojoRawTypeModel<Map>) introspector.typeModel( name )
		);
	}

	static <T> HibernateOrmGenericTypeModelFactory<T[]> array(HibernateOrmGenericTypeModelFactory<T> elementType) {
		return introspector -> SyntheticPojoGenericTypeModel.array(
				introspector.typeModel( Object[].class ),
				elementType.create( introspector )
		);
	}

	static <C extends Collection> HibernateOrmGenericTypeModelFactory<C> collection(
			Class<C> collectionType, HibernateOrmGenericTypeModelFactory<?> elementType) {
		return introspector -> SyntheticPojoGenericTypeModel.genericType(
				introspector.typeModel( collectionType ),
				elementType.create( introspector )
		);
	}

	static <M extends Map> HibernateOrmGenericTypeModelFactory<M> map(
			Class<M> mapType,
			HibernateOrmGenericTypeModelFactory<?> keyType,
			HibernateOrmGenericTypeModelFactory<?> valueType) {
		return introspector -> SyntheticPojoGenericTypeModel.genericType(
				introspector.typeModel( mapType ),
				keyType.create( introspector ),
				valueType.create( introspector )
		);
	}

}
