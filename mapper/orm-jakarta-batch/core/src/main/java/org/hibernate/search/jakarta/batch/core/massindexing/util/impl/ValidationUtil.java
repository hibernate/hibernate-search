/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.logging.impl.JakartaBatchLog;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;

/**
 * Utility class for job parameter validation.
 *
 * @author Mincong Huang
 */
public final class ValidationUtil {

	private ValidationUtil() {
		// Private constructor, do not use it.
	}

	public static void validateCheckpointInterval(int checkpointInterval, int rowsPerPartition) {
		if ( checkpointInterval > rowsPerPartition ) {
			throw JakartaBatchLog.INSTANCE.illegalCheckpointInterval( checkpointInterval, rowsPerPartition );
		}
	}

	public static void validateEntityFetchSize(int entityFetchSize, int checkpointInterval) {
		if ( entityFetchSize > checkpointInterval ) {
			throw JakartaBatchLog.INSTANCE.illegalEntityFetchSize( entityFetchSize, checkpointInterval );
		}
	}

	public static void validatePositive(String parameterName, int parameterValue) {
		if ( parameterValue <= 0 ) {
			throw JakartaBatchLog.INSTANCE.negativeValueOrZero( parameterName, parameterValue );
		}
	}

	public static void validateEntityTypes(
			EntityManagerFactoryRegistry emfRegistry,
			String entityManagerFactoryScope,
			String entityManagerFactoryReference,
			String serializedEntityTypes) {
		EntityManagerFactory emf = JobContextUtil.getEntityManagerFactory(
				emfRegistry,
				entityManagerFactoryScope,
				entityManagerFactoryReference
		);

		SearchMapping mapping = Search.mapping( emf );

		Set<String> failingTypes = new LinkedHashSet<>();
		for ( String serializedEntityType : serializedEntityTypes.split( "," ) ) {
			try {
				mapping.indexedEntity( serializedEntityType );
			}
			// if the type is not indexed, a SearchException is thrown
			catch (SearchException ex) {
				failingTypes.add( serializedEntityType );
			}
		}

		if ( failingTypes.size() > 0 ) {
			throw JakartaBatchLog.INSTANCE.failingEntityTypes( String.join( ", ", failingTypes ) );
		}
	}

}
