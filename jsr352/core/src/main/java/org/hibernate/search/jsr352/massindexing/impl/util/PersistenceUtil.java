/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.persistence.EmbeddedId;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.search.jsr352.massindexing.impl.steps.lucene.IndexScope;
import org.hibernate.search.util.StringHelper;

/**
 * Internal utility class for persistence usage.
 *
 * @author Mincong Huang
 */
public final class PersistenceUtil {

	/**
	 * The type of identifier(s) of a given {@link EntityType}.
	 */
	private enum IdType {
		/**
		 * The given entity type contains only one {@link Id} annotation.
		 */
		SINGLE_ID,
		/**
		 * The given entity type contains an {@link EmbeddedId} annotation.
		 */
		EMBEDDED_ID,
		/**
		 * The given entity type contains an {@link IdClass} annotation.
		 */
		ID_CLASS,
		/**
		 * The given entity type does not match any other case available from this enum. This should never happen.
		 */
		UNKNOWN
	}

	/**
	 * Internal ID processor for processing customized criterion.
	 */
	private enum IdProcessor {
		GE {

			@Override
			public <X> Criterion processIds(SingularAttribute<X, ?>[] idAttributes, Object idObj, String prefix)
					throws Exception {
				Conjunction[] or = new Conjunction[idAttributes.length];
				prefix = prefix != null ? prefix : "";

				for ( int i = 0; i < or.length; i++ ) {
					// Group expressions together in a single conjunction (A and B and C...).
					SimpleExpression[] and = new SimpleExpression[i + 1];
					int j = 0;
					for ( ; j < and.length - 1; j++ ) {
						// The first N-1 expressions have symbol `=`
						String key = idAttributes[j].getName();
						Object val = getProperty( idObj, key );
						and[j] = Restrictions.eq( prefix + key, val );
					}
					// The last expression has symbol `>=`
					String key = idAttributes[j].getName();
					Object val = getProperty( idObj, key );
					and[j] = Restrictions.ge( prefix + key, val );

					or[i] = Restrictions.conjunction( and );
				}
				// Group the disjunction of multiple expressions (X or Y or Z...).
				return Restrictions.or( or );
			}

			@Override
			public <X> Criterion processId(SingularAttribute<X, ?> idAttribute, Object obj) {
				return Restrictions.ge( idAttribute.getName(), obj );
			}
		},
		LT {

			@Override
			public <X> Criterion processIds(SingularAttribute<X, ?>[] idAttributes, Object idObj, String prefix)
					throws Exception {
				Conjunction[] or = new Conjunction[idAttributes.length];
				prefix = prefix != null ? prefix : "";

				for ( int i = 0; i < or.length; i++ ) {
					// Group expressions together in a single conjunction (A and B and C...).
					SimpleExpression[] and = new SimpleExpression[i + 1];
					int j = 0;
					for ( ; j < and.length - 1; j++ ) {
						// The first N-1 expressions have symbol `=`
						String key = idAttributes[j].getName();
						Object val = getProperty( idObj, key );
						and[j] = Restrictions.eq( prefix + key, val );
					}
					// The last expression has symbol `<`
					String key = idAttributes[j].getName();
					Object val = getProperty( idObj, key );
					and[j] = Restrictions.lt( prefix + key, val );

					or[i] = Restrictions.conjunction( and );
				}
				// Group the disjunction of multiple expressions (X or Y or Z...).
				return Restrictions.or( or );
			}

			@Override
			public <X> Criterion processId(SingularAttribute<X, ?> idAttribute, Object obj) {
				return Restrictions.lt( idAttribute.getName(), obj );
			}
		};

		/**
		 * Processes multiple ID attributes of the given ID object. This method should be used when target entity has
		 * multiple ID attributes, e.g. an entity annotated {@link IdClass}, or an entity having {@link EmbeddedId}.
		 *
		 * @param idAttributes An ID attributes array, <b>sorted</b> by the name of attribute.
		 * @param idObj The ID object to process, in which all the ID attributes should have a public getter method.
		 * @param prefix The ID property prefix. Only used for entity having {@link EmbeddedId}. In other case, set it
		 * to {@code null}.
		 * @param <X> The type containing the represented attribute.
		 * @return The customized criterion.
		 * @throws Exception When any exception occurs.
		 */
		public abstract <X> Criterion processIds(SingularAttribute<X, ?>[] idAttributes, Object idObj, String prefix)
				throws Exception;

		/**
		 * Processes a single ID attribute of the given ID object. This method should be used when target entity has a
		 * single ID attribute.
		 *
		 * @param idAttribute The unique ID attribute of this entity.
		 * @param idObj The ID object to process.
		 * @param <X> The type containing the represented attribute.
		 * @return The standard criterion.
		 */
		public abstract <X> Criterion processId(SingularAttribute<X, ?> idAttribute, Object idObj);
	}

	private PersistenceUtil() {
		// Private constructor, do not use it.
	}

	/**
	 * Open a session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a normal
	 * session will be returned. The entity manager factory should be not null and opened when calling this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 * @return a new session
	 */
	public static Session openSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		Session session;
		if ( StringHelper.isEmpty( tenantId ) ) {
			session = sessionFactory.openSession();
		}
		else {
			session = sessionFactory.withOptions()
					.tenantIdentifier( tenantId )
					.openSession();
		}
		return session;
	}

	/**
	 * Open a stateless session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a
	 * normal stateless session will be returned. The entity manager factory should be not null and opened when calling
	 * this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 * @return a new stateless session
	 */
	public static StatelessSession openStatelessSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		StatelessSession statelessSession;
		if ( StringHelper.isEmpty( tenantId ) ) {
			statelessSession = sessionFactory.openStatelessSession();
		}
		else {
			statelessSession = sessionFactory.withStatelessOptions()
					.tenantIdentifier( tenantId )
					.openStatelessSession();
		}
		return statelessSession;
	}

	/**
	 * Determines the index scope using the input parameters.
	 *
	 * @see IndexScope
	 */
	public static IndexScope getIndexScope(String hql, Set<Criterion> criterionSet) {
		if ( StringHelper.isNotEmpty( hql ) ) {
			return IndexScope.HQL;
		}
		else if ( criterionSet != null && criterionSet.size() > 0 ) {
			return IndexScope.CRITERIA;
		}
		else {
			return IndexScope.FULL_ENTITY;
		}
	}

	/**
	 * Creates a list of {@link Order} based on the ID attribute(s) inside the given entity type.
	 *
	 * @param entityManagerFactory Entity manager factory.
	 * @param entity The entity type.
	 * @param <X> The type containing the represented attribute.
	 * @return A list of {@link Order}, sorted by lexicographical order on the ID attributes' name.
	 */
	public static <X> List<Order> createIdOrders(EntityManagerFactory entityManagerFactory, Class<X> entity) {
		EntityType<X> entityType = entityManagerFactory.getMetamodel().entity( entity );
		List<SingularAttribute<?, ?>> attributeList;
		List<Order> orders = new ArrayList<>();

		switch ( getIdTypeOf( entityType ) ) {
			case SINGLE_ID:
				Class<?> idJavaType = entityType.getIdType().getJavaType();
				SingularAttribute<?, ?> idAttribute = entityType.getId( idJavaType );
				orders.add( Order.asc( idAttribute.getName() ) );
				return orders;

			case EMBEDDED_ID:
				Class<?> embeddable = entityType.getIdType().getJavaType();
				EmbeddableType<?> embeddableType = entityManagerFactory.getMetamodel().embeddable( embeddable );
				String embeddableName = entityType.getId( embeddable ).getName();

				attributeList = new ArrayList<>( embeddableType.getSingularAttributes() );
				attributeList.sort( Comparator.comparing( Attribute::getName ) );
				attributeList.forEach( attr -> {
					String propertyName = embeddableName + "." + attr.getName();
					orders.add( Order.asc( propertyName ) );
				} );
				return orders;

			case ID_CLASS:
				attributeList = new ArrayList<>( entityType.getIdClassAttributes() );
				attributeList.sort( Comparator.comparing( Attribute::getName ) );
				attributeList.forEach( attr -> orders.add( Order.asc( attr.getName() ) ) );
				return orders;

			default:
				throw new IllegalStateException( "Cannot determine IdType: this should never happen." );
		}
	}

	public static List<Criterion> createCriterionList(
			EntityManagerFactory entityManagerFactory,
			PartitionBound partitionBound) throws Exception {
		Class<?> entity = partitionBound.getEntityType();
		Metamodel metamodel = entityManagerFactory.getMetamodel();
		List<Criterion> result = new ArrayList<>();

		if ( partitionBound.hasUpperBound() ) {
			Object upperBound = partitionBound.getUpperBound();
			result.add( processCriterion( metamodel, entity, upperBound, IdProcessor.LT ) );
		}
		if ( partitionBound.hasLowerBound() ) {
			Object lowerBound = partitionBound.getLowerBound();
			result.add( processCriterion( metamodel, entity, lowerBound, IdProcessor.GE ) );
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <X> Criterion processCriterion(
			Metamodel metamodel,
			Class<X> entity,
			Object idObj,
			IdProcessor processor) throws Exception {
		EntityType<X> entityType = metamodel.entity( entity );
		List<SingularAttribute<?, ?>> attributeList;

		switch ( getIdTypeOf( entityType ) ) {
			case SINGLE_ID:
				Class<?> idJavaType = entityType.getIdType().getJavaType();
				return processor.processId( entityType.getId( idJavaType ), idObj );

			case EMBEDDED_ID:
				Class<?> embeddable = entityType.getIdType().getJavaType();
				EmbeddableType<?> embeddableType = metamodel.embeddable( embeddable );
				String embeddableName = entityType.getId( embeddable ).getName();
				String prefix = embeddableName + ".";

				attributeList = new ArrayList<>( embeddableType.getSingularAttributes() );
				attributeList.sort( Comparator.comparing( Attribute::getName ) );
				return processor.processIds( attributeList.toArray( new SingularAttribute[0] ), idObj, prefix );

			case ID_CLASS:
				attributeList = new ArrayList<>( entityType.getIdClassAttributes() );
				attributeList.sort( Comparator.comparing( Attribute::getName ) );
				return processor.processIds( attributeList.toArray( new SingularAttribute[0] ), idObj, null );

			default:
				throw new IllegalStateException( "Cannot determine IdType: this should never happen." );
		}
	}

	private static Object getProperty(Object obj, String propertyName)
			throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		return new PropertyDescriptor( propertyName, obj.getClass() ).getReadMethod().invoke( obj );
	}

	private static IdType getIdTypeOf(EntityType<?> entityType) {
		if ( entityType.hasSingleIdAttribute() ) {
			if ( entityType.getIdType().getPersistenceType() == Type.PersistenceType.EMBEDDABLE ) {
				return IdType.EMBEDDED_ID;
			}
			else {
				return IdType.SINGLE_ID;
			}
		}
		try {
			entityType.getIdClassAttributes();
			return IdType.ID_CLASS;
		}
		catch (IllegalArgumentException e) {
			// It should never happen.
			return IdType.UNKNOWN;
		}
	}

}
