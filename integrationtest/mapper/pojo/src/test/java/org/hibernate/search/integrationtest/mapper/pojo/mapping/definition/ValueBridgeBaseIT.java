/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) value bridges.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
 * <p>
 * Does not test field annotations in depth; this is tested in {@link FieldBaseIT}.
 */
@SuppressWarnings("unused")
public class ValueBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "integer", null ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexNullAs_noParsing() {
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

		SubTest.expectException( () -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class ) )
				.assertThrown()
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

		@Override
		public Integer cast(Object value) {
			return (Integer) value;
		}
	}

	public static class ParsingValueBridge extends NoParsingValueBridge {

		public ParsingValueBridge() {
		}

		@Override
		public Integer parse(String value) {
			return Integer.parseInt( value );
		}
	}
}
