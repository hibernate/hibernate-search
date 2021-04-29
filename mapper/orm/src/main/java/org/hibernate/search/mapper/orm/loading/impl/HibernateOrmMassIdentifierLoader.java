/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmMassIdentifierLoader<E, I> implements PojoMassIdentifierLoader {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TransactionManager transactionManager;
	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final HibernateOrmMassLoadingOptions options;
	private final PojoMassIdentifierSink<I> sink;
	private final SharedSessionContractImplementor session;
	private final long totalCount;
	private final boolean wrapInJtaTransaction;
	private long totalLoaded = 0;
	private final ScrollableResults results;

	public HibernateOrmMassIdentifierLoader(TransactionManager transactionManager,
			TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			HibernateOrmQueryLoader<E, I> typeQueryLoader,
			HibernateOrmMassLoadingOptions options,
			PojoMassIdentifierSink<I> sink,
			SharedSessionContractImplementor session) {
		this.transactionManager = transactionManager;
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.options = options;
		this.sink = sink;
		this.session = session;

		this.wrapInJtaTransaction = wrapInJtaTransaction();

		beginTransaction( options.idLoadingTransactionTimeout() );

		try {
			long objectsLimit = options.objectsLimit();
			long totalCountFromQuery = typeQueryLoader
					.createCountQuery( session )
					.setCacheable( false ).uniqueResult();
			if ( objectsLimit != 0 && objectsLimit < totalCountFromQuery ) {
				totalCount = objectsLimit;
			}
			else {
				totalCount = totalCountFromQuery;
			}

			if ( log.isDebugEnabled() ) {
				log.debugf( "going to fetch %d primary keys", totalCount );
			}

			results = typeQueryLoader.createIdentifiersQuery( session )
					.setCacheable( false )
					.setFetchSize( options.idFetchSize() )
					.scroll( ScrollMode.FORWARD_ONLY );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( HibernateOrmMassIdentifierLoader::commitTransaction, this );
			throw e;
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( HibernateOrmMassIdentifierLoader::commitTransaction, this );
			closer.push( ScrollableResults::close, results );
			closer.push( SharedSessionContractImplementor::close, session );
		}
	}

	@Override
	public long totalCount() {
		return totalCount;
	}

	@Override
	public void loadNext() {
		int batchSize = options.objectLoadingBatchSize();
		ArrayList<I> destinationList = new ArrayList<>( batchSize );
		while ( destinationList.size() < batchSize && totalLoaded < totalCount && results.next() ) {
			@SuppressWarnings("unchecked")
			I id = (I) results.get( 0 );
			destinationList.add( id );
			++totalLoaded;
		}

		if ( destinationList.isEmpty() ) {
			sink.complete();
		}
		else {
			// Explicitly checking whether the TX is still open; Depending on the driver implementation new ids
			// might be produced otherwise if the driver fetches all rows up-front
			if ( !session.isTransactionInProgress() ) {
				throw log.transactionNotActiveWhileProducingIdsForBatchIndexing();
			}
			sink.accept( destinationList );
		}
	}

	private void beginTransaction(Integer transactionTimeout) {
		try {
			if ( wrapInJtaTransaction ) {
				if ( transactionTimeout != null ) {
					transactionManager.setTransactionTimeout( transactionTimeout );
				}
				transactionManager.begin();
			}
			else {
				session.accessTransaction().begin();
			}
		}
		// Just let runtime exceptions fall through
		catch (NotSupportedException | SystemException e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private void commitTransaction() {
		try {
			if ( wrapInJtaTransaction ) {
				transactionManager.commit();
			}
			else {
				session.accessTransaction().commit();
			}
		}
		// Just let runtime exceptions fall through
		catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private boolean wrapInJtaTransaction() {
		if ( !transactionCoordinatorBuilder.isJta() ) {
			//Today we only require a TransactionManager on JTA based transaction factories
			log.trace( "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction" );
			return false;
		}
		if ( transactionManager == null ) {
			//no TM, nothing to do OR configuration mistake
			log.trace( "No TransactionManager found, do not start a surrounding JTA transaction" );
			return false;
		}
		try {
			if ( transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION ) {
				log.trace( "No Transaction in progress, needs to start a JTA transaction" );
				return true;
			}
		}
		catch (SystemException e) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in progress, no need to start a JTA transaction" );
		return false;
	}

}
