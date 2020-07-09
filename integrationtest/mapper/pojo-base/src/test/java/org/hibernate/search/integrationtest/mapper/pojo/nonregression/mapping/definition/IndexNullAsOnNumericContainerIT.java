/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.nonregression.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests a corner case that used to not work well in Search 5
 * where an index field created from a property whose type is a container of a numeric type
 * (e.g. {@code List<Integer>})
 * would result in indexNullAs being interpreted as a string rather than the numeric type.
 */
@TestForIssue(jiraKey = "HSEARCH-2663")
public class IndexNullAsOnNumericContainerIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void test() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				// Check that the field has type Integer and has an Integer indexNullAs value
				.field( "integerList", Integer.class, b2 -> b2.indexNullAs( 42 ).multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getIntegerList().add( 1 );
			entity1.getIntegerList().add( null );
			entity1.getIntegerList().add( 2 );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							// Check that null values are forwarded as null to the backend (not as a String)
							.field( "integerList", 1, null, 2 )
					)
					.processedThenExecuted();
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		private Integer id;

		private List<Integer> integerList = new ArrayList<>();

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField(indexNullAs = "42")
		public List<Integer> getIntegerList() {
			return integerList;
		}
	}
}
