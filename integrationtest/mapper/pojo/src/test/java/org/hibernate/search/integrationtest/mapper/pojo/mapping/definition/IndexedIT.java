/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("unused")
public class IndexedIT {

	@Rule
	public BackendMock defaultBackendMock = new BackendMock( "defaultBackend" );

	@Rule
	public BackendMock backend2Mock = new BackendMock( "backend2" );

	@Rule
	public BackendMock backend3Mock = new BackendMock( "backend3" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper =
			JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), defaultBackendMock );

	@Rule
	public JavaBeanMappingSetupHelper multiBackendSetupHelper =
			JavaBeanMappingSetupHelper.withBackendMocks(
					MethodHandles.lookup(), defaultBackendMock, backend2Mock, backend3Mock
			);

	@Test
	public void implicitIndexName() {
		@Indexed
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getName(), b -> b
				.field( "text", String.class )
		);
		setupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void explicitIndexName() {
		@Indexed(index = "explicitIndexName")
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( "explicitIndexName", b -> b
				.field( "text", String.class )
		);
		setupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void nonDefaultBackend() {
		@Indexed(index = "index", backend = "backend2")
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		backend2Mock.expectSchema( "index", b -> b
				.field( "text", String.class )
		);
		multiBackendSetupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void multiBackend() {
		@Indexed(index = "index1", backend = "backend2")
		class IndexedEntity1 {
			Integer id;
			String text1;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText1() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		@Indexed(index = "index2", backend = "backend3")
		class IndexedEntity2 {
			Integer id;
			String text2;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText2() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		backend2Mock.expectSchema( "index1", b -> b
				.field( "text1", String.class )
		);
		backend3Mock.expectSchema( "index2", b -> b
				.field( "text2", String.class )
		);
		multiBackendSetupHelper.start()
				.setup( IndexedEntity1.class, IndexedEntity2.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void error_indexedWithoutEntityMetadata() {
		@Indexed
		class IndexedWithoutEntityMetadata {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		SubTest.expectException(
				() -> setupHelper.start()
						// Do not mention the type is an entity type here, on purpose, to trigger the failure
						.withAnnotatedTypes( IndexedWithoutEntityMetadata.class )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedWithoutEntityMetadata.class.getName() )
						.failure(
								"Type '" + IndexedWithoutEntityMetadata.class.getName()
										+ "' is not marked as an entity type, yet it is indexed or targeted"
										+ " by an association from an indexed type. Please check your configuration."
						)
						.build()
				);
	}

	@Test
	public void error_indexedAbstractType_annotationMapping() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		abstract class AbstractIndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( AbstractIndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( AbstractIndexedEntity.class.getName() )
						.failure(
								"Cannot map type '" + AbstractIndexedEntity.class.getName() + "' to an index,"
										+ " because this type is abstract."
						)
						.build()
				);
	}

	@Test
	public void error_indexedAbstractType_programmaticMapping() {
		final String indexName = "indexName";
		abstract class AbstractIndexedEntity {
			Integer id;
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start()
						.withConfiguration( builder -> {
							builder.setAnnotatedTypeDiscoveryEnabled( false );
							builder.addEntityType( AbstractIndexedEntity.class );
							builder.programmaticMapping()
									.type( AbstractIndexedEntity.class )
									.indexed( indexName )
											.property( "id" ).documentId();
						} )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( AbstractIndexedEntity.class.getName() )
						.failure(
								"Cannot map type '" + AbstractIndexedEntity.class.getName() + "' to an index,"
										+ " because this type is abstract."
						)
						.build()
				);
	}

}
