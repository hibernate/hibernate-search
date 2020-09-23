/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.batch.runtime.context.JobContext;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.Predicate;

import org.hibernate.search.batch.jsr352.core.context.jpa.impl.ActiveSessionFactoryRegistry;
import org.hibernate.search.batch.jsr352.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Utility allowing to set up and retrieve the job context data, shared by all the steps.
 * <p>
 * When no {@link EntityManagerFactoryRegistry} is provided,
 * this utility uses an {@link ActiveSessionFactoryRegistry},
 * which has some limitations (see its javadoc).
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public final class JobContextUtil {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private JobContextUtil() {
		// Private constructor, do not use it.
	}

	public static JobContextData getOrCreateData(JobContext jobContext,
			EntityManagerFactoryRegistry emfRegistry,
			String entityManagerFactoryNamespace, String entityManagerFactoryReference,
			String entityTypes, String serializedCustomQueryCriteria) throws ClassNotFoundException, IOException {
		JobContextData data = (JobContextData) jobContext.getTransientUserData();
		if ( data == null ) {
			EntityManagerFactory emf = getEntityManagerFactory( emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference );
			data = createData( emf, entityTypes, serializedCustomQueryCriteria );
			jobContext.setTransientUserData( data );
		}
		return data;
	}

	public static JobContextData getExistingData(JobContext jobContext) {
		JobContextData data = (JobContextData) jobContext.getTransientUserData();
		if ( data == null ) {
			throw new AssertionFailure( "The job context data was unexpectedly missing;"
					+ " there probably is something wrong with how Hibernate Search set up the job context data." );
		}
		return data;
	}

	static EntityManagerFactory getEntityManagerFactory(EntityManagerFactoryRegistry emfRegistry,
			String entityManagerFactoryNamespace, String entityManagerFactoryReference) {
		EntityManagerFactoryRegistry registry =
				emfRegistry != null ? emfRegistry : ActiveSessionFactoryRegistry.getInstance();

		if ( StringHelper.isEmpty( entityManagerFactoryNamespace ) ) {
			if ( StringHelper.isEmpty( entityManagerFactoryReference ) ) {
				return registry.useDefault();
			}
			else {
				return registry.get( entityManagerFactoryReference );
			}
		}
		else {
			if ( StringHelper.isEmpty( entityManagerFactoryReference ) ) {
				throw log.entityManagerFactoryReferenceIsEmpty();
			}
			else {
				return registry.get( entityManagerFactoryNamespace, entityManagerFactoryReference );
			}
		}
	}

	private static JobContextData createData(EntityManagerFactory emf, String entityTypes, String serializedCustomQueryCriteria)
			throws ClassNotFoundException, IOException {
		SearchMapping mapping = Search.mapping( emf );
		List<String> entityNamesToIndex = Arrays.asList( entityTypes.split( "," ) );

		Set<Class<?>> entityTypesToIndex = entityNamesToIndex.stream()
				.map( mapping::indexedEntity )
				.map( ie -> ie.javaClass() )
				.collect( Collectors.toSet() );

		List<EntityTypeDescriptor> descriptors = PersistenceUtil.createDescriptors( emf, entityTypesToIndex );

		@SuppressWarnings("unchecked")
		Set<Predicate> criteria = SerializationUtil.parseParameter( Set.class, CUSTOM_QUERY_CRITERIA, serializedCustomQueryCriteria );
		if ( criteria == null ) {
			criteria = Collections.emptySet();
		}
		log.criteriaSize( criteria.size() );

		JobContextData jobContextData = new JobContextData();
		jobContextData.setEntityManagerFactory( emf );
		jobContextData.setCustomQueryCriteria( criteria );
		jobContextData.setEntityTypeDescriptors( descriptors );
		return jobContextData;
	}

}
