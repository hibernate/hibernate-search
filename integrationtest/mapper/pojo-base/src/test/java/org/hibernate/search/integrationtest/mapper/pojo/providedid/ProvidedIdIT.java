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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

public class ProvidedIdIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void indexAndSearch() {
		final String entityAndIndexName = "indexed";
		@Indexed
		class IndexedEntity {
		}

		// Schema
		backendMock.expectSchema( entityAndIndexName, b -> { } );
		SearchMapping mapping = withBaseConfiguration()
				.withAnnotatedEntityType( IndexedEntity.class, entityAndIndexName )
				.setup();
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			session.indexingPlan().add( "42", entity1 );

			backendMock.expectWorks( entityAndIndexName )
					.add( "42", b -> { } )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( entityAndIndexName ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( entityAndIndexName, "42" )
					)
			);

			SearchQuery<EntityReference> query = session.search( IndexedEntity.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			assertThat( query.fetchAll().hits() )
					.containsExactly( EntityReferenceImpl.withName( IndexedEntity.class, entityAndIndexName, "42" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void error_nullProvidedId() {
		final String entityAndIndexName = "indexed";
		@Indexed
		class IndexedEntity {
		}

		backendMock.expectAnySchema( entityAndIndexName );
		SearchMapping mapping = withBaseConfiguration()
				.withAnnotatedEntityType( IndexedEntity.class, entityAndIndexName )
				.setup();
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			Assertions.assertThatThrownBy(
					() -> session.indexingPlan().add( entity1 )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "the provided identifier was null" );
		}
	}

	private JavaBeanMappingSetupHelper.SetupContext withBaseConfiguration() {
		return setupHelper.start()
				.withConfiguration( b -> b.providedIdentifierBridge( BeanReference.of( NaiveIdentifierBridge.class ) ) );
	}

	public static class NaiveIdentifierBridge implements IdentifierBridge<Object> {
		private Map<String, Object> objectIds = new HashMap<>();

		@Override
		public String toDocumentIdentifier(Object propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			String documentId = propertyValue.toString();
			objectIds.put( documentId, propertyValue );
			return documentId;
		}

		@Override
		public Object fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
			return objectIds.get( documentIdentifier );
		}
	}
}
