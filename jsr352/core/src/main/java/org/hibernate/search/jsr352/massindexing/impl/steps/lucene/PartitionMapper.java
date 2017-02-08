/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.PartitionBound;
import org.jboss.logging.Logger;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production step: "produceLuceneDoc". The partition
 * plan is defined dynamically, according to the number of partitions given by the user.
 *
 * @author Mincong Huang
 */
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Logger LOGGER = Logger.getLogger( PartitionMapper.class );

	private enum Type {
		HQL, CRITERIA, FULL_ENTITY
	}

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty
	private String fetchSize;

	@Inject
	@BatchProperty
	private String hql;

	@Inject
	@BatchProperty
	private String maxThreads;

	@Inject
	@BatchProperty
	private String rowsPerPartition;

	private EntityManagerFactory emf;

	public PartitionMapper() {
	}

	/**
	 * Constructor for unit test. TODO should it be done in this way?
	 *
	 * @param emf
	 * @param fetchSize
	 * @param hql
	 * @param maxThreads
	 * @param rowsPerPartition
	 */
	PartitionMapper(EntityManagerFactory emf,
			String fetchSize,
			String hql,
			String rowsPerPartition,
			String maxThreads) {
		this.emf = emf;
		this.fetchSize = fetchSize;
		this.hql = hql;
		this.maxThreads = maxThreads;
		this.rowsPerPartition = rowsPerPartition;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		SessionFactory sessionFactory = null;
		Session session = null;
		StatelessSession ss = null;
		ScrollableResults scroll = null;

		try {
			emf = jobData.getEntityManagerFactory();
			sessionFactory = emf.unwrap( SessionFactory.class );
			session = sessionFactory.openSession();
			ss = sessionFactory.openStatelessSession();

			List<Class<?>> rootEntities = jobData.getEntityTypes();
			List<PartitionBound> partitionBounds = new ArrayList<>();
			Class<?> entityType;

			switch ( typeOfSelection( hql, jobData.getCriteria() ) ) {
				case HQL:
					entityType = rootEntities.get( 0 );
					partitionBounds.add( new PartitionBound( entityType, null, null ) );
					break;

				case CRITERIA:
					entityType = rootEntities.get( 0 );
					scroll = buildScrollableResults( ss, session, entityType, jobData.getCriteria() );
					partitionBounds = buildPartitionUnitsFrom( scroll, entityType );
					break;

				case FULL_ENTITY:
					for ( Class<?> clz : rootEntities ) {
						scroll = buildScrollableResults( ss, session, clz, null );
						partitionBounds.addAll( buildPartitionUnitsFrom( scroll, clz ) );
					}
					break;
			}
			jobData.setPartitionBounds( partitionBounds );

			// Build partition plan
			final int threads = Integer.valueOf( maxThreads );
			final int partitions = partitionBounds.size();
			final Properties[] props = new Properties[partitions];
			LOGGER.infof( "%d partitions, %d threads.", partitions, threads );

			for ( int i = 0; i < partitionBounds.size(); i++ ) {
				props[i] = new Properties();
				props[i].setProperty( "entityName", partitionBounds.get( i ).getEntityName() );
				props[i].setProperty( "partitionId", String.valueOf( i ) );
			}

			PartitionPlan partitionPlan = new PartitionPlanImpl();
			partitionPlan.setPartitionProperties( props );
			partitionPlan.setPartitions( partitions );
			partitionPlan.setThreads( threads );
			return partitionPlan;
		}
		finally {
			try {
				if ( scroll != null ) {
					scroll.close();
				}
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
			try {
				ss.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
			try {
				session.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}
	}

	private Type typeOfSelection(String hql, Set<Criterion> criterions) {
		if ( hql != null && !hql.isEmpty() ) {
			return Type.HQL;
		}
		else if ( criterions != null && criterions.size() > 0 ) {
			return Type.CRITERIA;
		}
		else {
			return Type.FULL_ENTITY;
		}
	}

	private List<PartitionBound> buildPartitionUnitsFrom(ScrollableResults scroll, Class<?> clazz) {
		List<PartitionBound> partitionUnits = new ArrayList<>();
		final int rowsPerPartition = Integer.parseInt( this.rowsPerPartition );
		Object lowerID = null;
		Object upperID = null;
		while ( scroll.scroll( rowsPerPartition ) ) {
			lowerID = upperID;
			upperID = scroll.get( 0 );
			partitionUnits.add( new PartitionBound( clazz, lowerID, upperID ) );
		}
		// add an additional partition on the tail
		lowerID = upperID;
		upperID = null;
		partitionUnits.add( new PartitionBound( clazz, lowerID, upperID ) );
		return partitionUnits;
	}

	private ScrollableResults buildScrollableResults(StatelessSession ss,
			Session session, Class<?> clazz, Set<Criterion> criterions) {
		Criteria criteria = ss.createCriteria( clazz );
		if ( criterions != null ) {
			criterions.forEach( c -> criteria.add( c ) );
		}
		ScrollableResults scroll = criteria
				.setProjection( Projections.alias( Projections.id(), "aliasedId" ) )
				.setFetchSize( Integer.parseInt( fetchSize ) )
				.setReadOnly( true )
				.addOrder( Order.asc( "aliasedId" ) )
				.scroll( ScrollMode.FORWARD_ONLY );
		return scroll;
	}
}
