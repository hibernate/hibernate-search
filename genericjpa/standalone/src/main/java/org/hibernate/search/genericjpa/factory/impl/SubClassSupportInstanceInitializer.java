/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.factory.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.engine.impl.SimpleInitializer;
import org.hibernate.search.genericjpa.annotations.InIndex;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * this initializer is needed i.e. for JPA implementations that subclass the entities, which would prevent
 * Hibernate-Search from indexing these, as it could otherwise not find any Annotations to process. <br>
 * <br>
 * All original Subclasses have to be annotated with {@link org.hibernate.search.genericjpa.annotations.InIndex}. <br>
 * <br>
 * all methods not related to getting Classes out of this are delegated to
 * {@link org.hibernate.search.impl.SimpleInitializer}
 *
 * @author Martin
 */
public class SubClassSupportInstanceInitializer implements InstanceInitializer {

	public static SubClassSupportInstanceInitializer INSTANCE = new SubClassSupportInstanceInitializer();

	private final SimpleInitializer initializer = SimpleInitializer.INSTANCE;

	private SubClassSupportInstanceInitializer() {

	}

	public Object unproxy(Object entity) {
		return this.initializer.unproxy( entity );
	}

	public Class<?> getClassFromWork(Work work) {
		return work.getEntityClass() != null ? work.getEntityClass() : getClass( work.getEntity() );
	}

	@SuppressWarnings("unchecked")
	public <T> Class<T> getClass(T entity) {
		// get the first class in the hierarchy that is actually in the index
		Class<T> clazz = (Class<T>) entity.getClass();
		if ( !clazz.isAnnotationPresent( InIndex.class ) ) {
			while ( (clazz = (Class<T>) clazz.getSuperclass()) != null ) {
				if ( clazz.isAnnotationPresent( InIndex.class ) ) {
					break;
				}
			}
		}
		if ( clazz != null ) {
			return clazz;
		}
		return this.initializer.getClass( entity );
	}

	public <T> Collection<T> initializeCollection(Collection<T> value) {
		return this.initializer.initializeCollection( value );
	}

	public <K, V> Map<K, V> initializeMap(Map<K, V> value) {
		return this.initializer.initializeMap( value );
	}

	public Object[] initializeArray(Object[] value) {
		return this.initializer.initializeArray( value );
	}

}
