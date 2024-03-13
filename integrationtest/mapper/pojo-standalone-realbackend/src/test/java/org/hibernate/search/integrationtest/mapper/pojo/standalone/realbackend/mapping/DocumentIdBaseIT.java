/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DocumentIdBaseIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withSingleBackend(
			MethodHandles.lookup(), BackendConfigurations.simple() );

	@Test
	void identifierBridge_Parse_fail() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyFailingStringBridge.class))
			String id;
		}

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.where( f -> f.id().matching( "anything", ValueConvert.PARSE ) )
					.fetchAllHits()
			).isInstanceOf( UnsupportedOperationException.class )
					.hasMessageContainingAll(
							"Should be called. But... Not implemented"
					);
		}
	}

	@Test
	void identifierBridge_Parse_pass() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyPassingStringBridge.class))
			String id;
		}

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThat(
					session.search( IndexedEntity.class )
							.where( f -> f.id().matching( "anything", ValueConvert.PARSE ) )
							.fetchAllHits()
			).isEmpty();
		}
	}

	@Test
	void identifierBridge_Parse_default() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Long id;
		}

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThat(
					session.search( IndexedEntity.class )
							.where( f -> f.id().matching( "1", ValueConvert.PARSE ) )
							.fetchAllHits()
			).isEmpty();
		}
	}

	public static class MyFailingStringBridge implements IdentifierBridge<String> {
		private static final String TOSTRING = "<MyStringBridge toString() result>";

		@Override
		public String fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public String toDocumentIdentifier(String propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public String parseIdentifierLiteral(String value) {
			throw new UnsupportedOperationException( "Should be called. But... Not implemented" );
		}

		@Override
		public String toString() {
			return TOSTRING;
		}
	}

	public static class MyPassingStringBridge implements IdentifierBridge<String> {
		private static final String TOSTRING = "<MyStringBridge toString() result>";

		@Override
		public String fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public String toDocumentIdentifier(String propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			return propertyValue;
		}

		@Override
		public String parseIdentifierLiteral(String value) {
			return value == null ? null : "parsed_" + value;
		}

		@Override
		public String toString() {
			return TOSTRING;
		}
	}
}
