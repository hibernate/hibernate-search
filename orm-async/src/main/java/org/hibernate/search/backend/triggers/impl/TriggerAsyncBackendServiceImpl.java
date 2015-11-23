/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.genericjpa.db.events.impl.AsyncUpdateSource;
import org.hibernate.search.genericjpa.db.events.index.impl.IndexUpdater;
import org.hibernate.search.genericjpa.db.events.jpa.impl.SQLJPAAsyncUpdateSourceProvider;
import org.hibernate.search.genericjpa.db.events.triggers.TriggerSQLStringSource;
import org.hibernate.search.genericjpa.entity.impl.ORMReusableEntityProvider;
import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.jpa.util.impl.ORMEntityManagerFactoryWrapper;
import org.hibernate.search.genericjpa.metadata.impl.MetadataRehasher;
import org.hibernate.search.genericjpa.metadata.impl.MetadataUtil;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;
import org.hibernate.search.spi.BuildContext;

import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATES_KEY;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_KEY;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.TRIGGER_SOURCE_KEY;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.UPDATE_DELAY_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants.UPDATE_DELAY_KEY;

/**
 * Created by Martin on 14.11.2015.
 */
public class TriggerAsyncBackendServiceImpl implements TriggerAsyncBackendService {

	private AsyncUpdateSource asyncUpdateSource;

	@Override
	public void start(
			SessionFactory sessionFactory,
			ExtendedSearchIntegrator extendedSearchIntegrator,
			ClassLoaderService cls,
			Properties properties) {
		Boolean enabled = Boolean.parseBoolean(
				(String) properties.getOrDefault(
						TriggerServiceConstants.TRIGGER_BASED_BACKEND_KEY,
						TriggerServiceConstants.TRIGGER_BASED_BACKEND_DEFAULT_VALUE
				)
		);
		if ( !enabled ) {
			return;
		}
		if ( this.asyncUpdateSource != null ) {
			throw new AssertionFailure( "TriggerAsyncBackendServiceImpl was started twice" );
		}

		List<Class<?>> entities = sessionFactory.getAllClassMetadata().entrySet().stream().map(
				e -> (Class<?>) e.getValue().getMappedClass()
		).collect( Collectors.toList() );
		List<Class<?>> indexRootTypes = entities.stream()
				.filter( cl -> cl.isAnnotationPresent( Indexed.class ) )
				.collect( Collectors.toList() );

		MetadataProvider metadataProvider = MetadataUtil.getDummyMetadataProvider( new StandaloneSearchConfiguration() );
		MetadataRehasher rehasher = new MetadataRehasher();
		List<RehashedTypeMetadata> rehashedTypeMetadatas = new ArrayList<>();

		Map<Class<?>, List<Class<?>>> containedInIndexOf;
		Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataPerIndexRoot;

		rehashedTypeMetadataPerIndexRoot = new HashMap<>();
		for ( Class<?> indexRootType : indexRootTypes ) {
			RehashedTypeMetadata rehashed = rehasher.rehash( metadataProvider.getTypeMetadataFor( indexRootType ) );
			rehashedTypeMetadatas.add( rehashed );
			rehashedTypeMetadataPerIndexRoot.put( indexRootType, rehashed );
		}
		containedInIndexOf = MetadataUtil.calculateInIndexOf( rehashedTypeMetadatas );
		Map<Class<?>, String> idProperties = MetadataUtil.calculateIdProperties( rehashedTypeMetadatas );

		SQLJPAAsyncUpdateSourceProvider asyncUpdateSourceProvider;
		{
			String triggerSource = (String) properties.get( TRIGGER_SOURCE_KEY );
			Class<?> triggerSourceClass;
			if ( triggerSource == null || (triggerSourceClass = cls.classForName( triggerSource )) == null ) {
				throw new SearchException(
						"class specified in " + TRIGGER_SOURCE_KEY + " could not be found: " + triggerSource
				);
			}
			String createTriggerStrategy = (String) properties.getOrDefault(
					TRIGGER_CREATION_STRATEGY_KEY,
					TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE
			);
			if ( !TRIGGER_CREATION_STRATEGY_CREATE.equals( createTriggerStrategy ) && !TRIGGER_CREATION_STRATEGY_DROP_CREATE
					.equals( createTriggerStrategy ) ) {
				throw new SearchException( "unrecognized " + TRIGGER_CREATION_STRATEGY_KEY + " specified: " + createTriggerStrategy );
			}
			try {
				asyncUpdateSourceProvider = new SQLJPAAsyncUpdateSourceProvider(
						(TriggerSQLStringSource) triggerSourceClass.newInstance(),
						entities, createTriggerStrategy
				);
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new SearchException( e );
			}
		}

		Integer batchSizeForUpdates = Integer
				.parseInt(
						(String) properties.getOrDefault(
								BATCH_SIZE_FOR_UPDATES_KEY,
								BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE
						)
				);
		Integer updateDelay = Integer.parseInt(
				(String) properties.getOrDefault(
						UPDATE_DELAY_KEY,
						UPDATE_DELAY_DEFAULT_VALUE
				)
		);

		this.asyncUpdateSource = asyncUpdateSourceProvider.getUpdateSource(
				updateDelay,
				TimeUnit.MILLISECONDS,
				batchSizeForUpdates,
				properties,
				new ORMEntityManagerFactoryWrapper( sessionFactory )
		);


		IndexUpdater indexUpdater = new IndexUpdater(
				rehashedTypeMetadataPerIndexRoot,
				containedInIndexOf,
				new ORMReusableEntityProvider( sessionFactory, idProperties ),
				extendedSearchIntegrator
		);

		this.asyncUpdateSource.setUpdateConsumers( Collections.singletonList( indexUpdater::updateEvent ) );

		this.asyncUpdateSource.start();
	}

	@Override
	public void stop() {
		if ( this.asyncUpdateSource != null ) {
			this.asyncUpdateSource.stop();
		}
		this.asyncUpdateSource = null;
	}

}
