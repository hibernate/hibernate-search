/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @*Field} annotations.
 * <p>
 * {@code @GenericField} is used in these tests, but other field annotations are expected to work the same,
 * because they rely on the same code internally.
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
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void valueBridge_default_noMatch() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			Object myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure( "No default value bridge implementation for type '"
										+ Object.class.getName() + "'",
								"Use a custom bridge" ) );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void valueBridge_default_noMatch_enumSuperClassRaw() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			Enum myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure( "No default value bridge implementation for type 'java.lang.Enum (java.lang.Enum<E>)'",
								"Use a custom bridge" ) );
	}

	@Test
	public void valueBridge_default_noMatch_enumSuperClassWithWildcard() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			Enum<?> myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure( "No default value bridge implementation for type 'java.lang.Enum<?>'",
								"Use a custom bridge" )
				);
	}

	@Test
	public void valueBridge_default_noMatch_enumSuperClassWithParameters() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			Enum<EnumForEnumSuperClassTest> myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure( "No default value bridge implementation for type 'java.lang.Enum<"
										+ EnumForEnumSuperClassTest.class.getName() + ">'",
								"Use a custom bridge" ) );
	}

	enum EnumForEnumSuperClassTest {
		VALUE1,
		VALUE2
	}

	@Test
	public void valueBridge_invalidInputType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField(valueBridge = @ValueBridgeRef(type = MyStringBridge.class))
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
								"This bridge expects an input of type '" + String.class.getName() + "'" ) );
	}

	@Test
	public void valueBridge_invalidInputType_implicitContainerExtractor() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = MyStringBridge.class))
			List<Integer> numbers;
		}
		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.failure( "Invalid bridge for input type '" + Integer.class.getName()
										+ "': '" + MyStringBridge.TOSTRING + "'",
								"This bridge expects an input of type '" + String.class.getName() + "'" ) );
	}

	public static class MyStringBridge implements ValueBridge<String, String> {
		private static final String TOSTRING = "<MyStringBridge toString() result>";
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
	public void valueBridge_valueBinder() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField(
					valueBridge = @ValueBridgeRef(name = "foo"),
					valueBinder = @ValueBinderRef(name = "bar")
			)
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( GenericField.class )
						.failure(
								"Ambiguous value bridge reference: both 'valueBridge' and 'valueBinder' are set."
										+ " Only one can be set."
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void valueBridge_implicitInputType_generic() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Unable to infer expected value type for value bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter V to 'T'."
								+ " The expected value type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the expected value type explicitly,"
								+ " or set the type parameter V to a definite, raw type." )
				);
	}

	public static class GenericTypeBridge<T> implements ValueBridge<T, String> {
		private static final String TOSTRING = "<GenericTypeBridge toString() result>";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public String toIndexedValue(T value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	@Test
	public void indexNullAs() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = ParsingValueBridge.class), indexNullAs = "7")
			Integer integer;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "integer", Integer.class, f -> f.indexNullAs( 7 ) )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "integer", null ) );
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
	public void indexNullAs_noParsing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = NoParsingValueBridge.class), indexNullAs = "7")
			Integer integer;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
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
