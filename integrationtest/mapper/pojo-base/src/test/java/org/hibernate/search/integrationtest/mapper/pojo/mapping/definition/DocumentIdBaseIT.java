/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBinderRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @DocumentId} annotation.
 *
 * Does not test default bridges, which are tested in {@link DocumentIdDefaultBridgeBaseIT}.
 */
@SuppressWarnings("unused")
public class DocumentIdBaseIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void identifierBridge_default_noMatch() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Object id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "No default identifier bridge implementation for type '" + Object.class.getName() + "'",
								"Use a custom bridge" ) );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void identifierBridge_default_noMatch_enumSuperClassRaw() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Enum id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "No default identifier bridge implementation for type 'java.lang.Enum'",
								"Use a custom bridge" ) );
	}

	@Test
	public void identifierBridge_default_noMatch_enumSuperClassWithWildcard() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Enum<?> id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "No default identifier bridge implementation for type 'java.lang.Enum<?>'",
								"Use a custom bridge" )
				);
	}

	@Test
	public void identifierBridge_default_noMatch_enumSuperClassWithParameters() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Enum<EnumForEnumSuperClassTest> id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "No default identifier bridge implementation for type 'java.lang.Enum<"
										+ EnumForEnumSuperClassTest.class.getName() + ">'",
								"Use a custom bridge" ) );
	}

	enum EnumForEnumSuperClassTest {
		VALUE1,
		VALUE2
	}

	@Test
	public void identifierBridge_invalidInputType() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyStringBridge.class))
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Invalid bridge for input type '" + Integer.class.getName()
										+ "': '" + MyStringBridge.TOSTRING + "'",
								"This bridge expects an input of type '" + String.class.getName() + "'." ) );
	}

	public static class MyStringBridge implements IdentifierBridge<String> {
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
		public String toString() {
			return TOSTRING;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void identifierBridge_invalidInputType_superType() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyNumberBridge.class))
			Object id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Invalid bridge for input type '" + Object.class.getName()
										+ "': '" + MyNumberBridge.TOSTRING + "'",
								"This bridge expects an input of type '" + Number.class.getName() + "'" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void identifierBridge_invalidInputType_subType() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyNumberBridge.class))
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Invalid bridge for input type '" + Integer.class.getName()
										+ "': '" + MyNumberBridge.TOSTRING + "'",
								"This bridge expects an input of type '" + Number.class.getName() + "'." ) );
	}

	public static class MyNumberBridge implements IdentifierBridge<Number> {
		private static final String TOSTRING = "<MyNumberBridge toString() result>";
		@Override
		public Number fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
		@Override
		public String toDocumentIdentifier(Number propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
		@Override
		public String toString() {
			return TOSTRING;
		}
	}

	@Test
	public void identifierBridge_identifierBinder() {
		@Indexed
		class IndexedEntity {
			@DocumentId(
					identifierBridge = @IdentifierBridgeRef(name = "foo"),
					identifierBinder = @IdentifierBinderRef(name = "bar")
			)
			Object id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( DocumentId.class )
						.failure(
								"Ambiguous identifier bridge reference: both 'identifierBridge' and 'identifierBinder' are set."
										+ " Only one can be set."
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void identifierBridge_implicitInputType_generic() {
		@Indexed
		class IndexedEntity {
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = GenericTypeBridge.class))
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Unable to infer expected identifier type for identifier bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements IdentifierBridge<I>,"
								+ " but sets the generic type parameter I to 'T'."
								+ " The expected identifier type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use an IdentifierBinder to set the expected identifier type explicitly,"
								+ " or set the type parameter I to a definite, raw type." )
				);
	}

	public static class GenericTypeBridge<T> implements IdentifierBridge<T> {
		private static final String TOSTRING = "<GenericTypeBridge toString() result>";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public String toDocumentIdentifier(T propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public T fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	@Test
	public void missing() {
		@Indexed
		class IndexedEntity {
			Object id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to define a document identifier for indexed type '"
										+ IndexedEntity.class.getName() + "'",
								"The property representing the entity identifier is unknown",
								"Define the document identifier explicitly by annotating"
										+ " a property whose values are unique with @DocumentId"
						) );
	}

}
