/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for {@link org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator#defaultReindexOnUpdate(ReindexOnUpdate)}.
 */
class DefaultReindexOnUpdateIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
	}

	/**
	 * If ReindexOnUpdate.DEFAULT is the default,
	 * and the inverse side of associations is correctly set up,
	 * then Hibernate Search will handle embedding *and* automatic reindexing.
	 */
	@Test
	void default_associationInverseSideKnown() {
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
		parent.id = 1;
		parent.value = "val1";

		ChildEntity child = new ChildEntity();
		child.id = 2;
		child.value = "val2";

		parent.child = child;
		child.parent = parent;

		// Test indexed-embedding
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( parent );

			backendMock.expectWorks( "ParentEntity" )
					.add( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					);
		}

		// Test automatic reindexing
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( child );

			backendMock.expectWorks( "ChildEntity" )
					.addOrUpdate( "2", b -> b
							.field( "value", "val2" )
					);
			// The child was updated, thus the parent (which index-embeds the childs) is reindexed.
			backendMock.expectWorks( "ParentEntity" )
					.addOrUpdate( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					);
		}
	}

	/**
	 * If ReindexOnUpdate.DEFAULT is the default,
	 * and the inverse side of associations is NOT correctly set up,
	 * then Hibernate Search bootstrap will fail.
	 */
	@Test
	void default_associationInverseSideUnknown() {
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration(
						builder -> {
							builder.defaultReindexOnUpdate( ReindexOnUpdate.DEFAULT );

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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( ParentEntity.class.getName() )
						.pathContext( ".child<no value extractors>.value<no value extractors>" )
						.failure(
								"Unable to find the inverse side of the association on type '" + ParentEntity.class.getName()
										+ "'"
										+ " at path '.child<no value extractors>'",
								"Hibernate Search needs this information in order to reindex '"
										+ ParentEntity.class.getName() + "' when '"
										+ ChildEntity.class.getName() + "' is modified."
						) );
	}

	/**
	 * If ReindexOnUpdate.NO is the default,
	 * even if the inverse side of associations is not correctly set up,
	 * then Hibernate Search will handle embedding, but not automatic reindexing.
	 */
	@Test
	void no_associationInverseSideUnknown() {
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
		parent.id = 1;
		parent.value = "val1";

		ChildEntity child = new ChildEntity();
		child.id = 2;
		child.value = "val2";

		parent.child = child;
		child.parent = parent;

		// Test indexed-embedding
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( parent );

			backendMock.expectWorks( "ParentEntity" )
					.add( "1", b -> b
							.field( "value", "val1" )
							.objectField( "child", b2 -> b2
									.field( "value", "val2" )
							)
					);
		}

		// Test automatic reindexing
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( child );

			backendMock.expectWorks( "ChildEntity" )
					.addOrUpdate( "2", b -> b
							.field( "value", "val2" )
					);
			// The child was updated, but automatic reindexing is disabled,
			// thus the parent (which index-embeds the childs) will NOT be reindexed.
		}
	}

	@SearchEntity
	public static final class ParentEntity {

		private Integer id;
		private String value;
		private ChildEntity child;

	}

	@SearchEntity
	public static final class ChildEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;
		private String value;
		private ParentEntity parent;

	}

}
