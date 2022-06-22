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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1656")
public class ImplementedInterfaceIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedPojo.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);

		mapping = setupHelper.start().setup( IndexedPojo.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
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
