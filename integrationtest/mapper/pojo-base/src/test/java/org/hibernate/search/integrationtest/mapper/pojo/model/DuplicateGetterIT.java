/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

/**
 * Test models with multiple getters for the same property.
 */
@TestForIssue(jiraKey = "HSEARCH-4117")
class DuplicateGetterIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void duplicateGetter_unmapped() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			private Integer id;

			@FullTextField
			private String text;

			private Boolean enabled;

			public boolean isEnabled() {
				return enabled != null && enabled;
			}

			public Boolean getEnabled() {
				return enabled;
			}
		}

		// Check that Hibernate Search starts correctly, ignoring the duplicate getter and not logging any warning
		logged.expectLevel( Level.WARN ).never();
		backendMock.expectSchema( indexName, b -> b
				.field( "text", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) ) );
		setupHelper.start().setup( IndexedEntity.class );
	}

	@Test
	void duplicateGetter_mapped() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			private Integer id;

			@FullTextField
			private String text;

			@GenericField
			private Boolean enabled;

			public boolean isEnabled() {
				return enabled != null && enabled;
			}

			public Boolean getEnabled() {
				return enabled;
			}
		}

		// Check that Hibernate Search starts correctly, selecting whatever getter comes first
		// but logging a warning
		logged.expectEvent( Level.WARN,
				"Multiple getters exist for property named 'enabled' on type '"
						+ IndexedEntity.class.getName() + "'",
				"Hibernate Search will use '", "'", " and ignore [", "]",
				"The selected getter may change from one startup to the next",
				"To get rid of this warning, either remove the extra getters"
						+ " or configure the access type for this property to 'FIELD'." );
		backendMock.expectSchema( indexName, b -> b
				.field( "text", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( "enabled", Boolean.class ) );
		setupHelper.start().setup( IndexedEntity.class );
	}

}
