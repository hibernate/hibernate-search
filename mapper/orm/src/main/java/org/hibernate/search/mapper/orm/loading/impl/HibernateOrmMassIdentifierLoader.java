/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.ArrayList;
import java.util.OptionalLong;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.orm.logging.impl.LoadingLog;
import org.hibernate.search.mapper.orm.logging.impl.OrmMiscLog;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.util.common.impl.Closer;

public final class HibernateOrmMassIdentifierLoader<E, I> implements PojoMassIdentifierLoader {

	private final HibernateOrmMassLoadingContext options;
	private final PojoMassIdentifierSink<I> sink;
	private final SharedSessionContractImplementor session;
	private final TransactionHelper transactionHelper;
	private final long totalCount;
	private long totalLoaded = 0;
	private final ScrollableResults<I> results;

	public HibernateOrmMassIdentifierLoader(HibernateOrmQueryLoader<E, I> typeQueryLoader,
			HibernateOrmMassLoadingContext options,
			PojoMassIdentifierSink<I> sink,
			SharedSessionContractImplementor session) {
		this.options = options;
		this.sink = sink;
		this.session = session;
		this.transactionHelper = new TransactionHelper( session.getFactory(), options.idLoadingTransactionTimeout() );

		transactionHelper.begin( session );

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

			if ( LoadingLog.INSTANCE.isDebugEnabled() ) {
				LoadingLog.INSTANCE.numberOfKeysToFetch( totalCount );
			}

			results = typeQueryLoader.createIdentifiersQuery( session )
					.setCacheable( false )
					.setFetchSize( options.idFetchSize() )
					.scroll( ScrollMode.FORWARD_ONLY );
		}
		catch (RuntimeException e) {
			transactionHelper.rollbackSafely( session, e );
			throw e;
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ScrollableResults::close, results );
			closer.push( h -> h.commit( session ), transactionHelper );
			closer.push( SharedSessionContractImplementor::close, session );
		}
	}

	@Override
	public OptionalLong totalCount() {
		return OptionalLong.of( totalCount );
	}

	@Override
	public void loadNext() throws InterruptedException {
		int batchSize = options.objectLoadingBatchSize();
		ArrayList<I> destinationList = new ArrayList<>( batchSize );
		while ( destinationList.size() < batchSize && totalLoaded < totalCount && results.next() ) {
			I id = results.get();
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
				throw OrmMiscLog.INSTANCE.transactionNotActiveWhileProducingIdsForBatchIndexing();
			}
			sink.accept( destinationList );
		}
	}

}
