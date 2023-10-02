/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Checks that Hibernate Search will fail to auto-detect the backend type and offer suggestions
 * if there are multiple backend types in the classpath.
 * Also checks that setting the property "hibernate.search.backend.type" will solve the problem.
 */
public abstract class AbstractBackendTypeAutoDetectMultipleBackendTypesInClasspathIT {

	protected AbstractBackendTypeAutoDetectMultipleBackendTypesInClasspathIT(String expectedBackendType,
			BackendConfiguration backendConfiguration) {
		this.expectedBackendType = expectedBackendType;
		this.ormSetupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	private final String expectedBackendType;
	@RegisterExtension
	private final OrmSetupHelper ormSetupHelper;

	@Test
	void backendType_notSet() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withBackendProperty( "type", null )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure( "Ambiguous backend type",
								"configuration property 'hibernate.search.backend.type' is not set,"
										+ " and multiple backend types are present in the classpath",
								"Set property 'hibernate.search.backend.type' to one of the following"
										+ " to select the backend type: [elasticsearch, lucene]" ) );
	}

	@Test
	void backendType_set() {
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withBackendProperty( "type", expectedBackendType )
				.setup( IndexedEntity.class );

		assertThat( Search.mapping( sessionFactory ).backend().getClass().getName() )
				.containsIgnoringCase( expectedBackendType );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static final class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
