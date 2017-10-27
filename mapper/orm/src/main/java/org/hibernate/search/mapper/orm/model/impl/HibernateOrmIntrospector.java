/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmIntrospector implements PojoIntrospector {

	private static final Log log = LoggerFactory.make( Log.class );

	private final SessionFactoryImplementor sessionFactoryImplementor;
	private final PropertyAccessStrategyResolver accessStrategyResolver;

	public HibernateOrmIntrospector(SessionFactoryImplementor sessionFactoryImplementor) {
		this.sessionFactoryImplementor = sessionFactoryImplementor;
		this.accessStrategyResolver = sessionFactoryImplementor.getServiceRegistry()
				.getService( PropertyAccessStrategyResolver.class );
	}

	@Override
	public <T> TypeModel<T> getEntityTypeModel(Class<T> type) {
		TypeModel<T> typeModel = tryCreateEntityTypeModel( type );
		if ( typeModel == null ) {
			// Not a managed class
			throw log.cannotMapNonEntityType( type );
		}
		return typeModel;
	}

	<T> TypeModel<T> createUnknownTypeModel(Class<T> type) {
		// TODO cache?
		TypeModel<T> typeModel = tryCreateEntityTypeModel( type );
		if ( typeModel == null ) {
			typeModel = tryCreateEmbeddableTypeModel( type );
		}
		if ( typeModel == null ) {
			typeModel = createNonManagedTypeModel( type );
		}
		return typeModel;
	}

	PropertyModel<?> createFallbackPropertyModel(TypeModel<?> holderTypeModel, String explicitAccessStrategyName,
			EntityMode entityMode, String propertyName) {
		Class<?> holderType = holderTypeModel.getJavaType();
		PropertyAccessStrategy accessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(
				holderType, explicitAccessStrategyName, entityMode
		);
		PropertyAccess propertyAccess = accessStrategy.buildPropertyAccess(
				holderType, propertyName
		);
		Getter getter = propertyAccess.getGetter();
		return new HibernateOrmPropertyModel<>( this, holderTypeModel, propertyName, getter );
	}

	private <T> TypeModel<T> tryCreateEntityTypeModel(Class<T> type) {
		try {
			EntityPersister persister = sessionFactoryImplementor.getMetamodel().entityPersister( type );
			return new EntityTypeModel<>( this, type, persister );
		}
		catch (MappingException ignored) {
			// The type is not an entity in the current session factory
			return null;
		}
	}

	private <T> TypeModel<T> tryCreateEmbeddableTypeModel(Class<T> type) {
		try {
			EmbeddableType<T> embeddableType = sessionFactoryImplementor.getMetamodel().embeddable( type );
			return new EmbeddableTypeModel<>( this, embeddableType );
		}
		catch (IllegalArgumentException ignored) {
			// The type is not embeddable in the current session factory
			return null;
		}
	}

	private <T> TypeModel<T> createNonManagedTypeModel(Class<T> type) {
		return new NonManagedTypeModel<>( this, type );
	}

	@Override
	// The actual class of a proxy of type T is always a Class<? extends T> (unless T is HibernateProxy, but we don't expect that)
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(T entity) {
		return Hibernate.getClass( entity );
	}

}
