/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @GenericField} annotation.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeBaseIT}.
 * <p>
 * Does not test uses of container value extractors, which are tested in {@link FieldContainerExtractorBaseIT}
 * (and others, see javadoc on that class).
 */
public class FieldBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void error_unableToResolveDefaultValueBridgeFromSourceType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			Object myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Object getMyProperty() {
				return myProperty;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to find a default value bridge implementation for type '"
										+ Object.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void error_unableToResolveDefaultValueBridgeFromSourceType_enumSuperClassRaw() {
		@Indexed
		class IndexedEntity {
			Integer id;
			Enum myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Enum getMyProperty() {
				return myProperty;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to find a default value bridge implementation for type 'java.lang.Enum'"
						)
						.build()
				);
	}

	@Test
	public void error_unableToResolveDefaultValueBridgeFromSourceType_enumSuperClassWithWildcard() {
		@Indexed
		class IndexedEntity {
			Integer id;
			Enum<?> myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Enum<?> getMyProperty() {
				return myProperty;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to find a default value bridge implementation for type 'java.lang.Enum<?>'"
						)
						.build()
				);
	}

	@Test
	public void error_unableToResolveDefaultValueBridgeFromSourceType_enumSuperClassWithParameters() {
		@Indexed
		class IndexedEntity {
			Integer id;
			Enum<EnumForEnumSuperClassTest> myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Enum<EnumForEnumSuperClassTest> getMyProperty() {
				return myProperty;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to find a default value bridge implementation for type 'java.lang.Enum<"
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
	public void error_invalidInputTypeForValueBridge() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@GenericField(valueBridge = @ValueBridgeRef(type = MyStringBridge.class))
			public Integer getId() {
				return id;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
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

	@Test
	public void error_invalidInputTypeForValueBridge_implicitContainerExtractor() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(valueBridge = @ValueBridgeRef(type = MyStringBridge.class))
			public List<Integer> getNumbers() {
				return numbers;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.failure(
								"Bridge '" + MyStringBridge.TOSTRING + "' cannot be applied to input type '"
										+ Integer.class.getName() + "'"
						)
						.build()
				);
	}

	public static class MyStringBridge implements ValueBridge<String, String> {
		private static String TOSTRING = "<MyStringBridge toString() result>";
		@Override
		public String toIndexedValue(String value,
				ValueBridgeToIndexedValueContext context) {
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
			Integer id;
			@DocumentId
			@GenericField(
					valueBridge = @ValueBridgeRef(name = "foo"),
					valueBinder = @ValueBinderRef(name = "bar")
			)
			public Integer getId() {
				return id;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( GenericField.class )
						.failure(
								"The field annotation defines both valueBridge and valueBinder."
										+ " Only one of those can be defined, not both."
						)
						.build()
				);
	}

	@Test
	public void indexNullAs() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer integer;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(valueBridge = @ValueBridgeRef(type = ParsingValueBridge.class), indexNullAs = "7")
			public Integer getInteger() { return integer; }
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "integer", Integer.class, f -> f.indexNullAs( 7 ) )
		);

		SearchMapping mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "integer", null ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	public static class ParsingValueBridge extends NoParsingValueBridge {

		public ParsingValueBridge() {
		}

		@Override
		public Integer parse(String value) {
			return Integer.parseInt( value );
		}
	}

	@Test
	public void error_indexNullAs_noParsing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer integer;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(valueBridge = @ValueBridgeRef(type = NoParsingValueBridge.class), indexNullAs = "7")
			public Integer getInteger() { return integer; }
		}

		Assertions.assertThatThrownBy( () -> setupHelper.start().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "does not support parsing a value from a String" )
				.hasMessageContaining( "integer" );
	}

	public static class NoParsingValueBridge implements ValueBridge<Integer, Integer> {

		public NoParsingValueBridge() {
		}

		@Override
		public Integer toIndexedValue(Integer value, ValueBridgeToIndexedValueContext context) {
			return value;
		}
	}
}
