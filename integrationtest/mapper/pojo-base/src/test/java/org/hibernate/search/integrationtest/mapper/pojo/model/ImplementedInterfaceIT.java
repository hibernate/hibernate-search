/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-1656")
class ImplementedInterfaceIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedPojo.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);

		mapping = setupHelper.start().setup( IndexedPojo.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedPojo indexedPojo = new IndexedPojo( 1, "Using some other text here" );
			session.indexingPlan().add( indexedPojo );

			backendMock.expectWorks( IndexedPojo.class.getSimpleName() )
					.add( "1", b -> b.field( "text", "Using some other text here" ) );
		}
	}

	public interface ImplementedInterface {

		@GenericField
		default String getText() {
			return "Index your POJOs with Hibernate Search 6";
		}
	}

	@Indexed
	public static class IndexedPojo implements ImplementedInterface {

		@DocumentId
		private Integer id;
		private String text;

		public IndexedPojo(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Override
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
