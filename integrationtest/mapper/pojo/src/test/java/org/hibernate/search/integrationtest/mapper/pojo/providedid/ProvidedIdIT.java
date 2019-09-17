/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.providedid;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class ProvidedIdIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void indexAndSearch() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
		}

		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> { } );
		SearchMapping mapping = withBaseConfiguration().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			session.indexingPlan().add( "42", entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "42", b -> { } )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( INDEX_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( INDEX_NAME, "42" )
					)
			);

			SearchQuery<EntityReference> query = session.search( IndexedEntity.class )
					.asEntityReference()
					.predicate( f -> f.matchAll() )
					.toQuery();

			assertThat( query.fetch().getHits() )
					.containsExactly( new EntityReferenceImpl( IndexedEntity.class, "42" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void error_nullProvidedId() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = withBaseConfiguration().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			SubTest.expectException(
					() -> session.indexingPlan().add( entity1 )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "the provided identifier was null" );
		}
	}

	private JavaBeanMappingSetupHelper.SetupContext withBaseConfiguration() {
		return setupHelper.start()
				.withConfiguration( b -> b.setImplicitProvidedId( true ) );
	}

}
