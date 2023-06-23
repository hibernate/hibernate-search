/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class DependencyRepeatableIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper
			.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4000")
	public void associationInverseSide() {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "keys", b2 -> b2
						.multiValued( true )
						.field( "text", String.class )
				)
				.objectField( "values", b2 -> b2
						.multiValued( true )
						.field( "text", String.class )
				)
		);

		SearchMapping mapping = setupHelper.start().setup( AssociationInverseSideRootEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			AssociationInverseSideRootEntity entity = new AssociationInverseSideRootEntity();
			entity.id = 1;
			entity.priceByEdition.put( new AssociationInverseSideKeyEntity( "bla", entity ),
					new AssociationInverseSideValueEntity( "blabla", entity ) );

			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.objectField( "keys", b2 -> b2.field( "text", "bla" ) )
							.objectField( "values", b2 -> b2.field( "text", "blabla" ) )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4000")
	public void indexingDependency() {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "keys", b2 -> b2
						.multiValued( true )
						.field( "text", String.class )
				)
				.objectField( "values", b2 -> b2
						.multiValued( true )
						.field( "text", String.class )
				)
		);

		SearchMapping mapping = setupHelper.start().setup( IndexingDependencyRootEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexingDependencyRootEntity entity = new IndexingDependencyRootEntity();
			entity.id = 1;
			entity.priceByEdition.put( new IndexingDependencyKeyEntity( "bla", entity ),
					new IndexingDependencyValueEntity( "blabla", entity ) );

			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.objectField( "keys", b2 -> b2.field( "text", "bla" ) )
							.objectField( "values", b2 -> b2.field( "text", "blabla" ) )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	private static class AssociationInverseSideRootEntity {
		@DocumentId
		private Integer id;

		@AssociationInverseSide(
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY),
				inversePath = @ObjectPath(@PropertyValue(propertyName = "rootEntity"))
		)
		@IndexedEmbedded(
				name = "keys",
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@AssociationInverseSide(
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE),
				inversePath = @ObjectPath(@PropertyValue(propertyName = "rootEntity"))
		)
		@IndexedEmbedded(
				name = "values",
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE)
		)
		private Map<AssociationInverseSideKeyEntity, AssociationInverseSideValueEntity> priceByEdition = new LinkedHashMap<>();
	}

	private static class AssociationInverseSideKeyEntity {
		@GenericField
		private String text;

		private AssociationInverseSideRootEntity rootEntity;

		public AssociationInverseSideKeyEntity(String text, AssociationInverseSideRootEntity rootEntity) {
			this.text = text;
			this.rootEntity = rootEntity;
		}
	}

	private static class AssociationInverseSideValueEntity {
		@GenericField
		private String text;

		private AssociationInverseSideRootEntity rootEntity;

		public AssociationInverseSideValueEntity(String text, AssociationInverseSideRootEntity rootEntity) {
			this.text = text;
			this.rootEntity = rootEntity;
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexingDependencyRootEntity {
		@DocumentId
		private Integer id;

		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.NO,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexedEmbedded(
				name = "keys",
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.NO,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE)
		)
		@IndexedEmbedded(
				name = "values",
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE)
		)
		private Map<IndexingDependencyKeyEntity, IndexingDependencyValueEntity> priceByEdition = new LinkedHashMap<>();
	}

	private static class IndexingDependencyKeyEntity {
		@GenericField
		private String text;

		private IndexingDependencyRootEntity rootEntity;

		public IndexingDependencyKeyEntity(String text, IndexingDependencyRootEntity rootEntity) {
			this.text = text;
			this.rootEntity = rootEntity;
		}
	}

	private static class IndexingDependencyValueEntity {
		@GenericField
		private String text;

		private IndexingDependencyRootEntity rootEntity;

		public IndexingDependencyValueEntity(String text, IndexingDependencyRootEntity rootEntity) {
			this.text = text;
			this.rootEntity = rootEntity;
		}
	}
}
