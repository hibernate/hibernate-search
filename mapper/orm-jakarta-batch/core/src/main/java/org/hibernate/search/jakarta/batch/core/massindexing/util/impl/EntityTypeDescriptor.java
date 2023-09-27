/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.query.SelectionQuery;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.orm.loading.spi.LoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class EntityTypeDescriptor<E, I> {

	public static <E> EntityTypeDescriptor<E, ?> create(LoadingTypeContext<E> type) {
		EntityIdentifierMapping identifierMapping = type.entityMappingType().getIdentifierMapping();
		IdOrder idOrder;
		if ( identifierMapping.getPartMappingType() instanceof EmbeddableMappingType ) {
			idOrder = new CompositeIdOrder<>( type );
		}
		else {
			idOrder = new SingularIdOrder<>( type );
		}
		return new EntityTypeDescriptor<>( type, type.loadingStrategy(), idOrder );
	}

	private final LoadingTypeContext<E> delegate;
	private final HibernateOrmEntityLoadingStrategy<? super E, I> loadingStrategy;
	private final IdOrder idOrder;

	public EntityTypeDescriptor(LoadingTypeContext<E> delegate,
			HibernateOrmEntityLoadingStrategy<? super E, I> loadingStrategy, IdOrder idOrder) {
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
		return loadingStrategy.createQueryLoader( List.of( delegate ), conditions, order );
	}

}
