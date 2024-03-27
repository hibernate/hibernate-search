/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests a corner case that used to not work well in Search 5
 * where an index field created from a property whose type is a container of a numeric type
 * (e.g. {@code List<Integer>})
 * would result in indexNullAs being interpreted as a string rather than the numeric type.
 */
@TestForIssue(jiraKey = "HSEARCH-2663")
class IndexNullAsOnNumericContainerIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void test() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				// Check that the field has type Integer and has an Integer indexNullAs value
				.field( "integerList", Integer.class, b2 -> b2.indexNullAs( 42 ).multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			entity1.integerList.add( 1 );
			entity1.integerList.add( null );
			entity1.integerList.add( 2 );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							// Check that null values are forwarded as null to the backend (not as a String)
							.field( "integerList", 1, null, 2 )
					);
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@GenericField(indexNullAs = "42")
		private List<Integer> integerList = new ArrayList<>();
	}
}
