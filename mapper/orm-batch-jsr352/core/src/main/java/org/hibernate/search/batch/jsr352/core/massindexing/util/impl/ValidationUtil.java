/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Utility class for job parameter validation.
 *
 * @author Mincong Huang
 */
public final class ValidationUtil {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ValidationUtil() {
		// Private constructor, do not use it.
	}

	public static void validateCheckpointInterval(int checkpointInterval, int rowsPerPartition) {
		if ( checkpointInterval > rowsPerPartition ) {
			throw log.illegalCheckpointInterval( checkpointInterval, rowsPerPartition );
		}
	}

	public static void validateSessionClearInterval(int sessionClearInterval, int checkpointInterval) {
		if ( sessionClearInterval > checkpointInterval ) {
			throw log.illegalSessionClearInterval( sessionClearInterval, checkpointInterval );
		}
	}

	public static void validatePositive(String parameterName, int parameterValue) {
		if ( parameterValue <= 0 ) {
			throw log.negativeValueOrZero( parameterName, parameterValue );
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
			throw log.failingEntityTypes( String.join( ", ", failingTypes ) );
		}
	}

}
