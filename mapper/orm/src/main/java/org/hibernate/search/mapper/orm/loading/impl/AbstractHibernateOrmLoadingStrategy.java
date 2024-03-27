/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public abstract class AbstractHibernateOrmLoadingStrategy<E, I>
		implements HibernateOrmEntityLoadingStrategy<E, I> {

	enum GroupingAllowed {
		NEVER {
			@Override
			public boolean allowed(boolean hasNonIndexedConcreteSubtypes) {
				return false;
			}
		},
		ALWAYS {
			@Override
			public boolean allowed(boolean hasNonIndexedConcreteSubtypes) {
				return true;
			}
		},
		ONLY_FOR_NO_NON_INDEXED_CONCRETE_SUBTYPES {
			@Override
			public boolean allowed(boolean hasNonIndexedConcreteSubtypes) {
				return !hasNonIndexedConcreteSubtypes;
			}
		};

		public static GroupingAllowed determine(PersistentClass persistentClass) {
			if ( isFromInheritanceType( persistentClass, JoinedSubclass.class ) ) {
				return ONLY_FOR_NO_NON_INDEXED_CONCRETE_SUBTYPES;
			}
			if ( isFromInheritanceType( persistentClass, SingleTableSubclass.class ) ) {
				return ALWAYS;
			}
			return NEVER;

		}

		private static boolean isFromInheritanceType(PersistentClass persistentClass, Class<? extends PersistentClass> kind) {
			return kind.isAssignableFrom( persistentClass.getClass() )
					|| persistentClass instanceof RootClass
							&& persistentClass.getSubclasses().stream()
									.anyMatch( c -> kind.isAssignableFrom( c.getClass() ) );
		}

		public abstract boolean allowed(boolean hasNonIndexedConcreteSubtypes);
	}

	protected final String rootEntityName;
	protected final Class<I> uniquePropertyType;
	protected final String uniquePropertyName;
	private final GroupingAllowed groupingAllowed;

	AbstractHibernateOrmLoadingStrategy(String rootEntityName, Class<I> uniquePropertyType, String uniquePropertyName,
			GroupingAllowed groupingAllowed) {
		this.rootEntityName = rootEntityName;
		this.uniquePropertyType = uniquePropertyType;
		this.uniquePropertyName = uniquePropertyName;
		this.groupingAllowed = groupingAllowed;
	}

	@Override
	public final PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoSelectionLoadingContext context) {
		var ormContext = (HibernateOrmSelectionLoadingContext) context;
		return createEntityLoader( expectedTypes, ormContext );
	}

	public abstract PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> targetEntityTypeContexts,
			HibernateOrmSelectionLoadingContext loadingContext);

	@Override
	public final boolean groupingAllowed(PojoLoadingTypeContext<? extends E> type, PojoMassLoadingContext context) {
		// Only allow grouping for types that don't have a conditional expression:
		// it's too complicated to apply a condition to multiple types in the same query.
		// TODO HSEARCH-4252 Apply a condition to multiple types in the same query

		// Also if we are looking at the InheritanceType.JOINED hierarchy and we are not targeting all subtypes
		// i.e. there's an @Indexed(enabled=false) on some subtype -- we don't want to join, as we'd be
		// creating queries with lots of joins and where clause containing `type in (???)`.
		// and in this case targeting a more specific type leads to generating a bit less joins
		return groupingAllowed.allowed( type.hasNonIndexedConcreteSubtypes() )
				&& ( (HibernateOrmMassLoadingContext) context ).conditionalExpression( type ).isEmpty();
	}

	@Override
	public final PojoMassIdentifierLoader createIdentifierLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassIdentifierLoadingContext<I> context) {
		var ormContext = (HibernateOrmMassLoadingContext) context.parent();
		SessionFactoryImplementor sessionFactory = ormContext.mapping().sessionFactory();

		HibernateOrmQueryLoader<E, I> queryLoader = createQueryLoader( sessionFactory, expectedTypes,
				conditionalExpressions( expectedTypes, ormContext ) );
		SharedSessionContractImplementor session = (SharedSessionContractImplementor) sessionFactory
				.withStatelessOptions()
				.tenantIdentifier( ormContext.tenancyConfiguration().convert( context.tenantIdentifier() ) )
				.openStatelessSession();
		try {
			PojoMassIdentifierSink<I> sink = context.createSink();
			return new HibernateOrmMassIdentifierLoader<>( queryLoader, ormContext, sink, session );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SharedSessionContractImplementor::close, session );
			throw e;
		}
	}

	@Override
	public final PojoMassEntityLoader<I> createEntityLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassEntityLoadingContext<E> context) {
		var ormContext = (HibernateOrmMassLoadingContext) context.parent();
		SessionFactoryImplementor sessionFactory = ormContext.mapping().sessionFactory();

		HibernateOrmQueryLoader<E, ?> queryLoader = createQueryLoader( sessionFactory, expectedTypes,
				conditionalExpressions( expectedTypes, ormContext ) );
		SessionImplementor session = (SessionImplementor) sessionFactory
				.withOptions()
				.tenantIdentifier( ormContext.tenancyConfiguration().convert( context.tenantIdentifier() ) )
				.openSession();
		try {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( ormContext.cacheMode() );
			session.setDefaultReadOnly( true );

			PojoMassEntitySink<E> sink = context.createSink( ormContext.mapping().sessionContext( session ) );
			return new HibernateOrmMassEntityLoader<>( queryLoader, ormContext, sink, session );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SessionImplementor::close, session );
			throw e;
		}
	}

	private List<ConditionalExpression> conditionalExpressions(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			HibernateOrmMassLoadingContext context) {
		if ( expectedTypes.size() != 1 ) {
			// We know there's no condition, see groupingAllowed()
			// TODO HSEARCH-4252 Apply a condition to multiple types in the same query
			return List.of();
		}
		var condition = context.conditionalExpression( expectedTypes.iterator().next() );
		return condition.isPresent() ? List.of( condition.get() ) : List.of();
	}

	@Override
	public HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions) {
		return createQueryLoader( sessionFactory, typeContexts, conditionalExpressions, null );
	}

	@Override
	public HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions, String order) {

		EntityMappingType commonSuperType = toMostSpecificCommonEntitySuperType( sessionFactory, typeContexts );
		if ( commonSuperType == null ) {
			throw invalidTypesException( typeContexts );
		}

		Set<Class<? extends E>> includedTypesFilter;
		if ( HibernateOrmUtils.targetsAllConcreteSubTypes( sessionFactory, commonSuperType, typeContexts ) ) {
			// All concrete types are included, no need to filter by type.
			includedTypesFilter = Collections.emptySet();
		}
		else {
			includedTypesFilter = new HashSet<>( typeContexts.size() );
			for ( PojoLoadingTypeContext<? extends E> typeContext : typeContexts ) {
				includedTypesFilter.add( typeContext.typeIdentifier().javaClass() );
			}
		}

		TypeQueryFactory<E, I> actualQueryFactory = createFactory( commonSuperType );

		if ( !conditionalExpressions.isEmpty() || order != null ) {
			if ( typeContexts.size() != 1 ) {
				// TODO HSEARCH-4252 Apply a condition to multiple types in the same query
				throw new AssertionFailure( "conditional/order expression is always defined on a single type" );
			}

			EntityMappingType entityMappingType = HibernateOrmUtils.entityMappingType( sessionFactory,
					typeContexts.iterator().next().secondaryEntityName() );
			return new HibernateOrmQueryLoaderImpl<>( actualQueryFactory, entityMappingType,
					includedTypesFilter, conditionalExpressions, order );
		}
		return new HibernateOrmQueryLoaderImpl<>( actualQueryFactory, includedTypesFilter );
	}

	protected abstract TypeQueryFactory<E, I> createFactory(Class<E> entityClass, String ormEntityName,
			Class<I> uniquePropertyType, String uniquePropertyName);

	@SuppressWarnings("unchecked")
	protected TypeQueryFactory<E, I> createFactory(EntityMappingType entityMappingType) {
		return createFactory( (Class<E>) entityMappingType.getJavaType().getJavaTypeClass(), entityMappingType.getEntityName(),
				uniquePropertyType, uniquePropertyName );
	}

	protected static EntityMappingType toMostSpecificCommonEntitySuperType(
			SessionFactoryImplementor sessionFactory,
			Iterable<? extends PojoLoadingTypeContext<?>> targetEntityTypeContexts) {
		EntityMappingType result = null;
		for ( PojoLoadingTypeContext<?> targetTypeContext : targetEntityTypeContexts ) {
			EntityMappingType type = HibernateOrmUtils.entityMappingType( sessionFactory,
					targetTypeContext.secondaryEntityName() );
			if ( result == null ) {
				result = type;
			}
			else {
				result = HibernateOrmUtils.toMostSpecificCommonEntitySuperType( result, type );
			}
		}
		return result;
	}

	protected org.hibernate.AssertionFailure invalidTypesException(
			Set<? extends PojoLoadingTypeContext<?>> targetEntityTypeContexts) {
		return new org.hibernate.AssertionFailure(
				"Some types among the targeted entity types are not subclasses of the expected root entity type."
						+ " Expected entity name: " + rootEntityName
						+ " Targeted entity names: "
						+ targetEntityTypeContexts.stream()
								.map( PojoLoadingTypeContext::secondaryEntityName )
								.collect( Collectors.toUnmodifiableList() )
		);
	}

}
