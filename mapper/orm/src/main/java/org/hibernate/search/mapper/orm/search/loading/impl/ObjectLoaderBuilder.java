/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;

public class ObjectLoaderBuilder<O> {

	private final Session session;
	private final Set<Class<? extends O>> concreteIndexedClasses;

	public ObjectLoaderBuilder(Session session, Set<Class<? extends O>> concreteIndexedClasses) {
		this.session = session;
		this.concreteIndexedClasses = concreteIndexedClasses;
	}

	public ObjectLoader<PojoReference, O> build(MutableObjectLoadingOptions mutableLoadingOptions) {
		return build( mutableLoadingOptions, Function.identity() );
	}

	public <T> ObjectLoader<PojoReference, T> build(MutableObjectLoadingOptions mutableLoadingOptions,
			Function<O, T> hitTransformer) {
		if ( concreteIndexedClasses.size() == 1 ) {
			Class<? extends O> concreteIndexedType = concreteIndexedClasses.iterator().next();
			return buildForSingleType( mutableLoadingOptions, concreteIndexedType, hitTransformer );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions, hitTransformer );
		}
	}

	private <T> ComposableObjectLoader<PojoReference, T> buildForSingleType(
			MutableObjectLoadingOptions mutableLoadingOptions, Class<? extends O> concreteIndexedType,
			Function<? super O, T> hitTransformer) {
		// TODO Add support for entities whose document ID is not the entity ID (natural ID, or other)
		// TODO Add support for other types of database retrieval and object lookup? See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.ObjectLoaderBuilder#getObjectInitializer
		return new SingleTypeByIdObjectLoader<>( session, concreteIndexedType, mutableLoadingOptions, hitTransformer );
	}

	private <T> ObjectLoader<PojoReference, T> buildForMultipleTypes(
			MutableObjectLoadingOptions mutableLoadingOptions, Function<? super O, T> hitTransformer) {
		/*
		 * TODO Group together entity types from a same hierarchy, so as to optimize loads
		 * (one query per entity hierarchy, and not one query per index).
		 */
		Map<Class<? extends O>, ComposableObjectLoader<PojoReference, ? extends T>> delegateByConcreteType =
				new HashMap<>( concreteIndexedClasses.size() );
		for ( Class<? extends O> concreteIndexedClass : concreteIndexedClasses ) {
			ComposableObjectLoader<PojoReference, T> delegate =
					buildForSingleType( mutableLoadingOptions, concreteIndexedClass, hitTransformer );
			delegateByConcreteType.put( concreteIndexedClass, delegate );
		}
		return new ByTypeObjectLoader<>( delegateByConcreteType );
	}

}
