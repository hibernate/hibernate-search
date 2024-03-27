/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.batch.runtime.context.JobContext;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.jakarta.batch.core.context.jpa.impl.ActiveSessionFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
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
			String entityTypes) {
		JobContextData data = (JobContextData) jobContext.getTransientUserData();
		if ( data == null ) {
			EntityManagerFactory emf =
					getEntityManagerFactory( emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference );
			data = createData( emf, entityTypes );
			jobContext.setTransientUserData( data );
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

	private static JobContextData createData(EntityManagerFactory emf, String entityTypes) {
		BatchMappingContext mapping = (BatchMappingContext) Search.mapping( emf );
		List<String> entityNamesToIndex = Arrays.asList( entityTypes.split( "," ) );

		Set<HibernateOrmLoadingTypeContext<?>> entityTypesToIndex = new LinkedHashSet<>();
		for ( String s : entityNamesToIndex ) {
			entityTypesToIndex.add( mapping.typeContextProvider().byEntityName().getOrFail( s ) );
		}

		List<EntityTypeDescriptor<?, ?>> descriptors = PersistenceUtil.createDescriptors(
				emf.unwrap( SessionFactoryImplementor.class ),
				entityTypesToIndex );

		JobContextData jobContextData = new JobContextData();
		jobContextData.setEntityManagerFactory( emf );
		jobContextData.setEntityTypeDescriptors( descriptors );
		jobContextData.setTenancyConfiguration( mapping.tenancyConfiguration() );
		return jobContextData;
	}

}
