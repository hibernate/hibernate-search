/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that annotations on interfaces are correctly detected
 * even when the interface is completely abstract as far as HSearch is concerned,
 * i.e. when no implementation is known by HSearch.
 */
@TestForIssue(jiraKey = "HSEARCH-4385")
public class AbstractInterfaceIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.objectField( "embedded", b2 -> b2
						.field( "text", String.class ) )
		);

		mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
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
