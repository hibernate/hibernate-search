/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.jpa.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer.UpdateEventInfo;
import org.hibernate.search.genericjpa.db.events.impl.AsyncUpdateSource;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.jpa.util.impl.EntityManagerFactoryWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.EntityManagerWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.MultiQueryAccess;
import org.hibernate.search.genericjpa.jpa.util.impl.MultiQueryAccess.ObjectIdentifierWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.QueryWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.TransactionWrapper;
import org.hibernate.search.genericjpa.util.NamingThreadFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * a {@link AsyncUpdateSource} implementation that uses JPA to retrieve the updates from the database. For this to work the
 * entities have to be setup with JPA annotations
 * <br>
 * <br>
 * <b>this implementation is async</b>
 *
 * @author Martin Braun
 */
public class JPAUpdateSource implements AsyncUpdateSource {

	private final String delimitedIdentifierToken;

	private static final Log log = LoggerFactory.make();

	private final List<EventModelInfo> eventModelInfos;
	private final EntityManagerFactoryWrapper emf;
	private final long timeOut;
	private final TimeUnit timeUnit;
	private final int batchSizeForUpdates;
	private final int batchSizeForDatabaseQueries;

	private final Map<String, EventModelInfo> updateTableToEventModelInfo;
	private final ScheduledExecutorService exec;
	private final ReentrantLock lock = new ReentrantLock();
	private List<UpdateConsumer> updateConsumers;
	private ScheduledFuture<?> job;
	private boolean cancelled = false;
	private boolean pause = false;

	/**
	 * this doesn't do real batching for the databasequeries
	 */
	public JPAUpdateSource(
			List<EventModelInfo> eventModelInfos,
			EntityManagerFactoryWrapper emf,
			long timeOut,
			TimeUnit timeUnit,
			int batchSizeForUpdates, String delimitedIdentifierToken) {
		this(
				eventModelInfos,
				emf,
				timeOut,
				timeUnit,
				batchSizeForUpdates,
				1,
				delimitedIdentifierToken,
				Executors.newSingleThreadScheduledExecutor( tf() )
		);
	}

	/**
	 * this does batching for databaseQueries according to what you set
	 */
	public JPAUpdateSource(
			List<EventModelInfo> eventModelInfos,
			EntityManagerFactoryWrapper emf,
			long timeOut,
			TimeUnit timeUnit,
			int batchSizeForUpdates,
			int batchSizeForDatabaseQueries,
			String delimitedIdentifierToken) {
		this(
				eventModelInfos,
				emf,
				timeOut,
				timeUnit,
				batchSizeForUpdates,
				batchSizeForDatabaseQueries,
				delimitedIdentifierToken,
				Executors
						.newSingleThreadScheduledExecutor( tf() )
		);
	}

	/**
	 * this does batching for databaseQueries according to what you set
	 */
	public JPAUpdateSource(
			List<EventModelInfo> eventModelInfos,
			EntityManagerFactoryWrapper emf,
			long timeOut,
			TimeUnit timeUnit,
			int batchSizeForUpdates,
			int batchSizeForDatabaseQueries,
			String delimitedIdentifierToken,
			ScheduledExecutorService exec) {
		this.eventModelInfos = eventModelInfos;
		this.emf = emf;
		if ( timeOut <= 0 ) {
			throw new IllegalArgumentException( "timeout must be greater than 0" );
		}
		this.timeOut = timeOut;
		this.timeUnit = timeUnit;
		if ( batchSizeForUpdates <= 0 ) {
			throw new IllegalArgumentException( "batchSize must be greater than 0" );
		}
		this.batchSizeForUpdates = batchSizeForUpdates;
		this.batchSizeForDatabaseQueries = batchSizeForDatabaseQueries;
		this.updateTableToEventModelInfo = new HashMap<>();
		for ( EventModelInfo info : eventModelInfos ) {
			this.updateTableToEventModelInfo.put( info.getUpdateTableName(), info );
		}
		if ( exec == null ) {
			throw new IllegalArgumentException( "the ScheduledExecutorService may not be null!" );
		}
		this.exec = exec;
		this.delimitedIdentifierToken = delimitedIdentifierToken;
	}

	private static ThreadFactory tf() {
		return new NamingThreadFactory( "JPAUpdateSource Thread" );
	}

	public static MultiQueryAccess query(
			JPAUpdateSource updateSource,
			EntityManagerWrapper em) {
		Map<String, Long> countMap = new HashMap<>();
		Map<String, QueryWrapper> queryMap = new HashMap<>();
		for ( EventModelInfo evi : updateSource.eventModelInfos ) {
			long count;
			{
				count = ((Number) em.createNativeQuery(
						"SELECT count(*) " + updateSource.fromPart( evi )
				).getSingleResult()).longValue();
			}
			countMap.put( evi.getUpdateTableName(), count );

			{
				//SELECT part
				StringBuilder queryString = new StringBuilder().append( "SELECT " )
						.append( updateSource.escape( "t1" ) )
						.append( "." )
						.append( updateSource.escape( evi.getUpdateIdColumn() ) )
						.append( ", " )
						.append( updateSource.escape( "t1" ) )
						.append( "." )
						.append( updateSource.escape( evi.getEventTypeColumn() ) );
				for ( EventModelInfo.IdInfo idInfo : evi.getIdInfos() ) {
					for ( String column : idInfo.getColumnsInUpdateTable() ) {
						queryString.append( ", " )
								.append( updateSource.escape( "t1" ) )
								.append( "." )
								.append( updateSource.escape( column ) );
					}
				}
				//FROM PART
				queryString.append( updateSource.fromPart( evi ) );

				//ORDER BY part
				queryString.append(
						" ORDER BY "
				).append( updateSource.escape( "t1" ) )
						.append( "." )
						.append( updateSource.escape( evi.getUpdateIdColumn() ) )
						.append( " ASC" );

				log.trace( "querying for updates: " + queryString.toString() );
				QueryWrapper query = em.createNativeQuery(
						queryString.toString()
				);
				queryMap.put( evi.getUpdateTableName(), query );
			}
		}
		return new MultiQueryAccess(
				countMap, queryMap, (first, second) -> {
			int res = Long.compare( updateSource.id( first ), updateSource.id( second ) );
			if ( res == 0 ) {
				throw new IllegalStateException( "database contained two update entries with the same id!" );
			}
			return res;
		}, updateSource.batchSizeForDatabaseQueries
		);
	}

	@Override
	public void setUpdateConsumers(List<UpdateConsumer> updateConsumers) {
		this.updateConsumers = updateConsumers;
	}

	@Override
	public void start() {
		if ( this.updateConsumers == null ) {
			throw new IllegalStateException( "updateConsumers was null!" );
		}
		this.cancelled = false;
		this.job = this.exec.scheduleWithFixedDelay(
				() -> {
					this.lock.lock();
					try {
						if ( this.pause ) {
							return;
						}
						if ( this.cancelled ) {
							return;
						}
						if ( !this.emf.isOpen() ) {
							return;
						}
						EntityManagerWrapper em = null;
						try {
							em = this.emf.createEntityManager();
							TransactionWrapper tx = em.getTransaction();
							tx.begin();
							try {
								MultiQueryAccess query = query( this, em );
								List<UpdateEventInfo> updateInfos = new ArrayList<>( this.batchSizeForUpdates );

								Map<String, Long> lastUpdateIdPerTable = new HashMap<>();

								long processed = 0;
								while ( query.next() ) {
									// we have no order problems here since
									// the query does the ordering for us
									Object[] valuesFromQuery = (Object[]) query.get();

									Long updateId = ((Number) valuesFromQuery[0]).longValue();
									Integer eventType = ((Number) valuesFromQuery[1]).intValue();

									EventModelInfo evi = this.updateTableToEventModelInfo.get( query.identifier() );

									lastUpdateIdPerTable.put( query.identifier(), updateId );

									//we skip the id and eventtype
									int currentIndex = 2;
									for ( EventModelInfo.IdInfo info : evi.getIdInfos() ) {
										ColumnType[] columnTypes = info.getColumnTypes();
										String[] columnNames = info.getColumnsInUpdateTable();
										Object val[] = new Object[columnTypes.length];
										for ( int i = 0; i < columnTypes.length; ++i ) {
											val[i] = valuesFromQuery[currentIndex++];
										}
										Object entityId = info.getIdConverter().convert(
												val,
												columnNames,
												columnTypes
										);
										//hack, info at annotation level
										//is string only, but on the programmatic
										//level Map<String, Object> is needed
										//so we abuse Java collections here.
										Map hints = info.getHints();
										updateInfos.add(
												new UpdateEventInfo(
														info.getEntityClass(),
														entityId,
														eventType,
														(Map<String, Object>) hints
												)
										);
									}
									// TODO: maybe move this to a method as
									// it is getting reused
									if ( ++processed % this.batchSizeForUpdates == 0 ) {
										for ( UpdateConsumer consumer : this.updateConsumers ) {
											consumer.updateEvent( updateInfos );
											log.trace( "handled update-event: " + updateInfos );
										}
										updateInfos.clear();
									}
								}
								if ( updateInfos.size() > 0 ) {
									for ( UpdateConsumer consumer : this.updateConsumers ) {
										consumer.updateEvent( updateInfos );
										log.trace( "handled update-event: " + updateInfos );
									}
									updateInfos.clear();
								}


								for ( Map.Entry<String, Long> toDelete : lastUpdateIdPerTable.entrySet() ) {
									String tableName = toDelete.getKey();
									Long updateId = toDelete.getValue();
									EventModelInfo evi = this.updateTableToEventModelInfo.get( tableName );
									String queryString = "DELETE FROM " + this.escape( tableName ) + " WHERE " + this.escape(
											evi.getUpdateIdColumn()
									) + " < " + (updateId + 1);
									log.trace( "deleting handled updates: " + queryString );
									em.createNativeQuery(
											queryString
									).executeUpdate();
								}

								if ( processed > 0 ) {
									log.trace( "processed " + processed + " updates" );
								}

								em.flush();
								// clear memory :)
								em.clear();

								tx.commit();
							}
							catch (Throwable e) {
								tx.rollback();
								throw e;
							}
						}
						catch (Exception e) {
							throw new SearchException( "Error occured during Update processing!", e );
						}
						finally {
							if ( em != null ) {
								em.close();
							}
						}
					}
					catch (Exception e) {
						log.exceptionOccurred( "Exception occured in JPAUpdateSource", e );
					}
					finally {
						this.lock.unlock();
					}
				}, 0, this.timeOut, this.timeUnit
		);
	}

	private Long id(ObjectIdentifierWrapper val) {
		return ((Number) ((Object[]) val.object)[0]).longValue();
	}

	@Override
	public void stop() {
		// first cancel the update job and wait for it to be done.
		if ( this.job != null ) {
			this.lock.lock();
			try {
				this.cancelled = true;
				this.job.cancel( false );
			}
			finally {
				this.lock.unlock();
			}
		}
		// and shutdown the executorservice
		if ( this.exec != null ) {
			this.exec.shutdown();
		}
	}

	@Override
	public void pause(boolean pause) {
		this.lock.lock();
		try {
			this.pause = pause;
		}
		finally {
			this.lock.unlock();
		}
	}

	private String fromPart(EventModelInfo evi) {
		StringBuilder queryString = new StringBuilder();
		JPAUpdateSource updateSource = this;
		//FROM part
		queryString.append( " FROM " )
				.append( updateSource.escape( evi.getUpdateTableName() ) )
				.append( " " )
				.append( updateSource.escape( "t1" ) )
				.append( " " );
		//INNER JOIN part
		{
			queryString.append( " INNER JOIN ( " )
					.append( " SELECT max(" )
					.append( updateSource.escape( "t2" ) )
					.append( "." )
					.append( updateSource.escape( evi.getUpdateIdColumn() ) )
					.append( ") " )
					.append( updateSource.escape( "updateid" ) );
			for ( EventModelInfo.IdInfo idInfo : evi.getIdInfos() ) {
				for ( String column : idInfo.getColumnsInUpdateTable() ) {
					queryString.append( ", " )
							.append( updateSource.escape( "t2" ) )
							.append( "." )
							.append( updateSource.escape( column ) );
				}
			}
			queryString.append( " FROM " )
					.append( updateSource.escape( evi.getUpdateTableName() ) )
					.append( " " )
					.append( updateSource.escape( "t2" ) );
			queryString.append( " GROUP BY " );
			{
				int i = 0;
				for ( EventModelInfo.IdInfo idInfo : evi.getIdInfos() ) {
					for ( String column : idInfo.getColumnsInUpdateTable() ) {
						if ( i++ > 0 ) {
							queryString.append( ", " );
						}
						queryString.append( updateSource.escape( "t2" ) )
								.append( "." )
								.append( updateSource.escape( column ) );
					}
				}
			}
			queryString.append( " ) " ).append( updateSource.escape( "t3" ) )
					.append( " ON " ).append( updateSource.escape( "t1" ) )
					.append( "." )
					.append( updateSource.escape( evi.getUpdateIdColumn() ) )
					.append( " = " )
					.append( updateSource.escape( "t3" ) )
					.append( "." ).append(
					updateSource.escape(
							"updateid"
					)
			);
		}
		return queryString.toString();
	}

	private String escape(String str) {
		return new StringBuilder().append( this.delimitedIdentifierToken )
				.append( str )
				.append( this.delimitedIdentifierToken )
				.toString();
	}

}
