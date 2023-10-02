/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.query.SelectionQuery;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class EntityTypeDescriptor<E, I> {

	public static <E> EntityTypeDescriptor<E, ?> create(SessionFactoryImplementor sessionFactory,
			HibernateOrmLoadingTypeContext<E> type) {
		EntityIdentifierMapping identifierMapping = type.entityMappingType().getIdentifierMapping();
		IdOrder idOrder;
		if ( identifierMapping.getPartMappingType() instanceof EmbeddableMappingType ) {
			idOrder = new CompositeIdOrder<>( type );
		}
		else {
			idOrder = new SingularIdOrder<>( type );
		}
		return new EntityTypeDescriptor<>( sessionFactory, type, type.loadingStrategy(), idOrder );
	}

	private final SessionFactoryImplementor sessionFactory;
	private final HibernateOrmLoadingTypeContext<E> delegate;
	private final HibernateOrmEntityLoadingStrategy<? super E, I> loadingStrategy;
	private final IdOrder idOrder;

	public EntityTypeDescriptor(SessionFactoryImplementor sessionFactory, HibernateOrmLoadingTypeContext<E> delegate,
			HibernateOrmEntityLoadingStrategy<? super E, I> loadingStrategy, IdOrder idOrder) {
		this.sessionFactory = sessionFactory;
		this.delegate = delegate;
		this.loadingStrategy = loadingStrategy;
		this.idOrder = idOrder;
	}

	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return delegate.typeIdentifier();
	}

	public Class<E> javaClass() {
		return delegate.typeIdentifier().javaClass();
	}

	public String jpaEntityName() {
		return delegate.jpaEntityName();
	}

	public IdOrder idOrder() {
		return idOrder;
	}

	public SelectionQuery<Long> createCountQuery(SharedSessionContractImplementor session,
			List<ConditionalExpression> conditions) {
		return queryLoader( conditions, null ).createCountQuery( session );
	}

	public SelectionQuery<I> createIdentifiersQuery(SharedSessionContractImplementor session,
			List<ConditionalExpression> conditions) {
		return queryLoader( conditions, idOrder.ascOrder() ).createIdentifiersQuery( session );
	}

	public SelectionQuery<? super E> createLoadingQuery(SessionImplementor session, String idParameterName) {
		return queryLoader( List.of(), null ).createLoadingQuery( session, idParameterName );
	}

	private HibernateOrmQueryLoader<? super E, I> queryLoader(List<ConditionalExpression> conditions, String order) {
		return loadingStrategy.createQueryLoader( sessionFactory, Set.of( delegate.delegate() ), conditions, order );
	}

}
