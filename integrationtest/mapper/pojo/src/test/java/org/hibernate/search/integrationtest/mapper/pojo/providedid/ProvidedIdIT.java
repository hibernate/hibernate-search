/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.providedid;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.util.SearchException;
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
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void indexAndSearch() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
		}

		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> { } );
		JavaBeanMapping mapping = withBaseConfiguration().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();

			manager.getMainWorkPlan().add( "42", entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "42", b -> { } )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( INDEX_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( INDEX_NAME, "42" )
					)
			);

			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class )
					.query()
					.asReference()
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			assertThat( query )
					.hasHitsExactOrder( new PojoReferenceImpl( IndexedEntity.class, "42" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void error_nullProvidedId() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
		}

		backendMock.expectAnySchema( INDEX_NAME );
		JavaBeanMapping mapping = withBaseConfiguration().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();

			SubTest.expectException(
					() -> manager.getMainWorkPlan().add( entity1 )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "the provided identifier was null" );
		}
	}

	private JavaBeanMappingSetupHelper.SetupContext withBaseConfiguration() {
		return setupHelper.withBackendMock( backendMock )
				.withConfiguration( b -> b.setImplicitProvidedId( true ) );
	}

}
