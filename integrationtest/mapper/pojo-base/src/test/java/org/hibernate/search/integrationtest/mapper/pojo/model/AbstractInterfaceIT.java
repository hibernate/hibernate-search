/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that annotations on interfaces are correctly detected
 * even when the interface is completely abstract as far as HSearch is concerned,
 * i.e. when no implementation is known by HSearch.
 */
@TestForIssue(jiraKey = "HSEARCH-4385")
class AbstractInterfaceIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.objectField( "embedded", b2 -> b2
						.field( "text", String.class ) )
		);

		mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity( 1, new AbstractInterface() {
				@Override
				public String getText() {
					return "Using some other text here";
				}
			} );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( IndexedEntity.class.getSimpleName() )
					.add( "1", b -> b.objectField( "embedded", b2 -> b2
							.field( "text", "Using some other text here" ) ) );
		}
	}

	public interface AbstractInterface {
		@GenericField
		String getText();
	}

	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public Integer id;
		@IndexedEmbedded
		public AbstractInterface embedded;

		public IndexedEntity(Integer id, AbstractInterface embedded) {
			this.id = id;
			this.embedded = embedded;
		}
	}
}
