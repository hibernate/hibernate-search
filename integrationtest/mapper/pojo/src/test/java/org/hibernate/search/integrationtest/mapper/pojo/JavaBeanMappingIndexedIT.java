/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class JavaBeanMappingIndexedIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

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
			@Field
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup(
						CollectionHelper.asLinkedHashSet(), // Do not mention the type here, on purpose, to trigger the failure
						CollectionHelper.asLinkedHashSet( IndexedWithoutEntityMetadata.class )
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
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
				() -> setupHelper.withBackendMock( backendMock ).setup( AbstractIndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( AbstractIndexedEntity.class.getName() )
						.annotationContextAnyParameters( Indexed.class )
						.failure(
								"Cannot map type '" + AbstractIndexedEntity.class.getName() + "' to index 'indexName',"
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
				() -> setupHelper.withBackendMock( backendMock ).setup( mappingRepositoryBuilder -> {
					JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create(
							mappingRepositoryBuilder, MethodHandles.lookup(),
							false, false
					);
					initiator.addEntityType( AbstractIndexedEntity.class );
					initiator.programmaticMapping()
							.type( AbstractIndexedEntity.class )
							.indexed( indexName )
									.property( "id" ).documentId();
					return initiator;
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( AbstractIndexedEntity.class.getName() )
						.failure(
								"Cannot map type '" + AbstractIndexedEntity.class.getName() + "' to index 'indexName',"
										+ " because this type is abstract."
						)
						.build()
				);
	}

}
