/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

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
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @DocumentId} annotation.
 *
 * Does not test default bridges, which are tested in {@link DocumentIdDefaultBridgeIT}.
 */
@SuppressWarnings("unused")
public class DocumentIdBaseIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void error_unableToResolveDefaultIdentifierBridgeFromSourceType() {
		@Indexed
		class IndexedEntity {
			Object id;
			@DocumentId
			public Object getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Unable to find a default identifier bridge implementation for type '"
										+ Object.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void error_unableToResolveDefaultIdentifierBridgeFromSourceType_enumSuperClassRaw() {
		@Indexed
		class IndexedEntity {
			Enum id;
			@DocumentId
			public Enum getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Unable to find a default identifier bridge implementation for type 'java.lang.Enum'"
						)
						.build()
				);
	}

	@Test
	public void error_unableToResolveDefaultIdentifierBridgeFromSourceType_enumSuperClassWithWildcard() {
		@Indexed
		class IndexedEntity {
			Enum<?> id;
			@DocumentId
			public Enum<?> getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Unable to find a default identifier bridge implementation for type 'java.lang.Enum<?>'"
						)
						.build()
				);
	}

	@Test
	public void error_unableToResolveDefaultIdentifierBridgeFromSourceType_enumSuperClassWithParameters() {
		@Indexed
		class IndexedEntity {
			Enum<EnumForEnumSuperClassTest> id;
			@DocumentId
			public Enum<EnumForEnumSuperClassTest> getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Unable to find a default identifier bridge implementation for type 'java.lang.Enum<"
										+ EnumForEnumSuperClassTest.class.getName() + ">'"
						)
						.build()
				);
	}

	enum EnumForEnumSuperClassTest {
		VALUE1,
		VALUE2
	}

	@Test
	public void error_invalidInputTypeForIdentifierBridge() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyStringBridge.class))
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Bridge '" + MyStringBridge.TOSTRING + "' cannot be applied to input type '"
										+ Integer.class.getName() + "'"
						)
						.build()
				);
	}

	public static class MyStringBridge implements IdentifierBridge<String> {
		private static String TOSTRING = "<MyStringBridge toString() result>";
		@Override
		public String cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
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
	public void error_definingBothBridgeReferenceAndBinderReference() {
		@Indexed
		class IndexedEntity {
			Object id;
			@DocumentId(
					identifierBridge = @IdentifierBridgeRef(name = "foo"),
					identifierBinder = @IdentifierBinderRef(name = "bar")
			)
			public Object getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( DocumentId.class )
						.failure(
								"Annotation @DocumentId on property 'id' defines both identifierBridge and identifierBinder."
										+ " Only one of those can be defined, not both."
						)
						.build()
				);
	}

	@Test
	public void error_missing() {
		@Indexed
		class IndexedEntity {
			Object id;
			public Object getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"There isn't any explicit document ID mapping for indexed type '"
										+ IndexedEntity.class.getName() + "',"
										+ " and the entity ID cannot be used as a default because it is unknown"
						)
						.build()
				);
	}

}
