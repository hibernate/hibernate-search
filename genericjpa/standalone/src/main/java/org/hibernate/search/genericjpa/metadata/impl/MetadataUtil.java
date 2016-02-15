/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.metadata.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.IndexingMode;

/**
 * @author Martin Braun
 */
public class MetadataUtil {

	private MetadataUtil() {
		throw new AssertionFailure( "can't touch this!" );
	}

	public static MetadataProvider getDummyMetadataProvider(SearchConfiguration searchConfiguration) {
		ConfigContext configContext = new ConfigContext(
				searchConfiguration, new BuildContext() {

			@Override
			public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
				return null;
			}

			@Override
			public String getIndexingStrategy() {
				return IndexingMode.EVENT.toExternalRepresentation();
			}

			@Override
			public IndexingMode getIndexingMode() {
				return IndexingMode.EVENT;
			}

			@Override
			public ServiceManager getServiceManager() {
				return new StandardServiceManager( searchConfiguration, this, Environment.DEFAULT_SERVICES_MAP );
			}

			@Override
			public IndexManagerHolder getAllIndexesManager() {
				return new IndexManagerHolder();
			}

			@Override
			public ErrorHandler getErrorHandler() {
				return new LogErrorHandler();
			}

		}
		);
		return new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	public static Map<Class<?>, String> calculateIdProperties(List<RehashedTypeMetadata> rehashedTypeMetadatas) {
		Map<Class<?>, String> idProperties = new HashMap<>();
		for ( RehashedTypeMetadata rehashed : rehashedTypeMetadatas ) {
			idProperties.putAll( rehashed.getIdPropertyNameForType() );
		}
		return idProperties;
	}

	/**
	 * calculates the Entity-Classes that are relevant for the indexes represented by the rehashedTypeMetadatas
	 *
	 * @return all Entity-Classes found in the rehashedTypeMetadatas
	 */
	public static Set<Class<?>> calculateIndexRelevantEntities(List<RehashedTypeMetadata> rehashedTypeMetadatas) {
		Set<Class<?>> indexRelevantEntities = new HashSet<>();
		for ( RehashedTypeMetadata rehashed : rehashedTypeMetadatas ) {
			indexRelevantEntities.addAll( rehashed.getIdPropertyNameForType().keySet() );
		}
		return indexRelevantEntities;
	}

	/**
	 * calculates a map that contains a list of all Entity-Classes in which the keyed Entity-Class is contained in
	 */
	public static Map<Class<?>, List<Class<?>>> calculateInIndexOf(List<RehashedTypeMetadata> rehashedTypeMetadatas) {
		Map<Class<?>, List<Class<?>>> inIndexOf = new HashMap<>();
		for ( RehashedTypeMetadata rehashed : rehashedTypeMetadatas ) {
			Class<?> rootType = rehashed.getOriginalTypeMetadata().getType();
			for ( Class<?> type : rehashed.getIdPropertyNameForType().keySet() ) {
				inIndexOf.computeIfAbsent( type, (key) -> new ArrayList<>() ).add( rootType );
			}
		}
		return inIndexOf;
	}

}
