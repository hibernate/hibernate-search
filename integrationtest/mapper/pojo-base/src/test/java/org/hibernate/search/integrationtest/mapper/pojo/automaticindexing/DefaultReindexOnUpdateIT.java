/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for {@link org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator#setDefaultReindexOnUpdate(ReindexOnUpdate)}.
 */
public class DefaultReindexOnUpdateIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
	}

	/**
	 * If ReindexOnUpdate.DEFAULT is the default,
	 * and the inverse side of associations is correctly set up,
	 * then Hibernate Search will handle embedding *and* automatic reindexing.
	 */
	@Test
	public void default_associationInverseSideKnown() {
		backendMock.expectSchema( "ParentEntity", b -> b
				.field( "value", String.class )
				.objectField( "child", b2 -> b2
						.field( "value", String.class )
				)
		);
		backendMock.expectSchema( "ChildEntity", b -> b
				.field( "value", String.class )
		);

		mapping = setupHelper.start()
				.withConfiguration(
						builder -> {
							builder.defaultReindexOnUpdate( ReindexOnUpdate.DEFAULT );

							builder.addEntityType( ParentEntity.class );
							builder.addEntityType( ChildEntity.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							TypeMappingStep parentMapping = mappingDefinition.type( ParentEntity.class );
							parentMapping.indexed();
							parentMapping.property( "id" ).documentId();
							parentMapping.property( "value" ).genericField();
							parentMapping.property( "child" )
									.associationInverseSide( PojoModelPath.ofValue( "parent" ) )
									.indexedEmbedded();
							TypeMappingStep childMapping = mappingDefinition.type( ChildEntity.class );
							childMapping.indexed();
							childMapping.property( "id" ).documentId();
							childMapping.property( "value" ).genericField();
						}
				)
				.setup();

		backendMock.verifyExpectationsMet();

		ParentEntity parent = new ParentEntity();
		parent.setId( 1 );
		parent.setValue( "val1" );

		ChildEntity child = new ChildEntity();
		child.setId( 2 );
		child.setValue( "val2" );

		parent.setChild( child );
		child.setParent( parent );

		// Test indexed-embedding
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( parent );

			backendMock.expectWorks( "ParentEntity" )
					.add( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					)
					.processedThenExecuted();
		}

		// Test automatic reindexing
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( child );

			backendMock.expectWorks( "ChildEntity" )
					.update( "2", b -> b
							.field( "value", "val2" )
					)
					.processedThenExecuted();
			// The child was updated, thus the parent (which index-embeds the childs) is reindexed.
			backendMock.expectWorks( "ParentEntity" )
					.update( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					)
					.processedThenExecuted();
		}
	}

	/**
	 * If ReindexOnUpdate.DEFAULT is the default,
	 * and the inverse side of associations is NOT correctly set up,
	 * then Hibernate Search bootstrap will fail.
	 */
	@Test
	public void default_associationInverseSideUnknown() {
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration(
						builder -> {
							builder.defaultReindexOnUpdate( ReindexOnUpdate.DEFAULT );

							builder.addEntityType( ParentEntity.class );
							builder.addEntityType( ChildEntity.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							TypeMappingStep parentMapping = mappingDefinition.type( ParentEntity.class );
							parentMapping.indexed();
							parentMapping.property( "id" ).documentId();
							parentMapping.property( "value" ).genericField();
							parentMapping.property( "child" )
									// Do NOT mention the inverse side of the association here.
									.indexedEmbedded();
							TypeMappingStep childMapping = mappingDefinition.type( ChildEntity.class );
							childMapping.indexed();
							childMapping.property( "id" ).documentId();
							childMapping.property( "value" ).genericField();
						}
				)
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( ParentEntity.class.getName() )
						.pathContext( ".child<no value extractors>.value<no value extractors>" )
						.failure(
								"Cannot find the inverse side of the association on type '" + ParentEntity.class.getName() + "'"
										+ " at path '.child<no value extractors>'",
								"Hibernate Search needs this information in order to reindex '"
										+ ParentEntity.class.getName() + "' when '"
										+ ChildEntity.class.getName() + "' is modified."
						)
						.build()
				);
	}
	/**
	 * If ReindexOnUpdate.NO is the default,
	 * even if the inverse side of associations is not correctly set up,
	 * then Hibernate Search will handle embedding, but not automatic reindexing.
	 */
	@Test
	public void no_associationInverseSideUnknown() {
		backendMock.expectSchema( "ParentEntity", b -> b
				.field( "value", String.class )
				.objectField( "child", b2 -> b2
						.field( "value", String.class )
				)
		);
		backendMock.expectSchema( "ChildEntity", b -> b
				.field( "value", String.class )
		);

		mapping = setupHelper.start()
				.withConfiguration(
						builder -> {
							builder.defaultReindexOnUpdate( ReindexOnUpdate.NO );

							builder.addEntityType( ParentEntity.class );
							builder.addEntityType( ChildEntity.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							TypeMappingStep parentMapping = mappingDefinition.type( ParentEntity.class );
							parentMapping.indexed();
							parentMapping.property( "id" ).documentId();
							parentMapping.property( "value" ).genericField();
							parentMapping.property( "child" )
									// Do NOT mention the inverse side of the association here.
									.indexedEmbedded();
							TypeMappingStep childMapping = mappingDefinition.type( ChildEntity.class );
							childMapping.indexed();
							childMapping.property( "id" ).documentId();
							childMapping.property( "value" ).genericField();
						}
				)
				.setup();

		backendMock.verifyExpectationsMet();

		ParentEntity parent = new ParentEntity();
		parent.setId( 1 );
		parent.setValue( "val1" );

		ChildEntity child = new ChildEntity();
		child.setId( 2 );
		child.setValue( "val2" );

		parent.setChild( child );
		child.setParent( parent );

		// Test indexed-embedding
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( parent );

			backendMock.expectWorks( "ParentEntity" )
					.add( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					)
					.processedThenExecuted();
		}

		// Test automatic reindexing
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( child );

			backendMock.expectWorks( "ChildEntity" )
					.update( "2", b -> b
							.field( "value", "val2" )
					)
					.processedThenExecuted();
			// The child was updated, but automatic reindexing is disabled,
			// thus the parent (which index-embeds the childs) will NOT be reindexed.
		}
	}

	public static final class ParentEntity {

		private Integer id;
		private String value;
		private ChildEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public ChildEntity getChild() {
			return child;
		}

		public void setChild(ChildEntity child) {
			this.child = child;
		}
	}

	public static final class ChildEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;
		private String value;
		private ParentEntity parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public ParentEntity getParent() {
			return parent;
		}

		public void setParent(ParentEntity parent) {
			this.parent = parent;
		}
	}

}
