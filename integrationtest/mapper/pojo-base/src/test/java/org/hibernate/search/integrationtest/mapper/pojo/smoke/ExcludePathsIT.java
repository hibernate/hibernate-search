/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.smoke;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge.CustomPropertyBinding;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge.CustomTypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ExcludePathsIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "customBridgeOnClass", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "customBridgeOnProperty", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "myEmbedded", b2 -> b2
						.objectField( "customBridgeOnClass", b3 -> b3
								.field( "date", LocalDate.class )
						)
						.objectField( "customBridgeOnProperty", b3 -> b3
								.field( "date", LocalDate.class )
								.field( "text", String.class )
						)
						.field( "myLocalDateField", LocalDate.class )
						.field( "myTextField", String.class )
						.objectField( "myEmbedded", b3 -> b3
								.objectField( "customBridgeOnClass", b4 -> b4
										.field( "date", LocalDate.class )
								)
								.objectField( "customBridgeOnProperty", b4 -> b4
										.field( "date", LocalDate.class )
										.field( "text", String.class )
								)
								.field( "myLocalDateField", LocalDate.class )
								.field( "myTextField", String.class )
						)
				)
				.field( "myTextField", String.class )
				.field( "myLocalDateField", LocalDate.class )
		);

		mapping = setupHelper.start()
				.expectCustomBeans()
				.withConfiguration( builder -> {
					builder.addEntityTypes( CollectionHelper.asSet(
							IndexedEntity.class
					) );
					builder.annotationMapping().add( IndexedEntity.class );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "this is text (1)" );
			entity1.setLocalDate( LocalDate.of( 2017, 11, 1 ) );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );
			entity2.setLocalDate( LocalDate.of( 2017, 11, 2 ) );
			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setText( "some more text (3)" );
			entity3.setLocalDate( LocalDate.of( 2017, 11, 3 ) );
			IndexedEntity entity6 = new IndexedEntity();
			entity6.setId( 6 );
			entity6.setText( "some more text (6)" );
			entity6.setLocalDate( LocalDate.of( 2017, 11, 6 ) );

			entity1.setEmbedded( entity2 );
			entity2.getEmbeddingAsSingle().add( entity1 );

			entity2.setEmbedded( entity3 );
			entity3.getEmbeddingAsSingle().add( entity2 );

			entity3.setEmbedded( entity2 );
			entity2.getEmbeddingAsSingle().add( entity3 );


			Map<String, List<IndexedEntity>> embeddedMap = new LinkedHashMap<>();
			embeddedMap.computeIfAbsent( "entity3", ignored -> new ArrayList<>() ).add( entity3 );
			embeddedMap.computeIfAbsent( "entity2", ignored -> new ArrayList<>() ).add( entity2 );
			embeddedMap.computeIfAbsent( "entity2", ignored -> new ArrayList<>() ).add( entity3 );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );
			session.indexingPlan().delete( entity1 );
			session.indexingPlan().add( entity3 );
			session.indexingPlan().add( entity6 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "2", b -> b
							.field( "myLocalDateField", entity2.getLocalDate() )
							.field( "myTextField", entity2.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity2.getText() )
									.field( "date", entity2.getLocalDate() )
							)
							.objectField( "customBridgeOnProperty", b2 -> b2
									.field( "text", entity2.getEmbedded().getText() )
									.field( "date", entity2.getEmbedded().getLocalDate() )
							)
							.objectField( "myEmbedded", b2 -> b2
									.field( "myTextField", entity2.getEmbedded().getText() )
									.field( "myLocalDateField", entity2.getEmbedded().getLocalDate() )
									.objectField( "customBridgeOnClass", b3 -> b3
											.field( "date", entity2.getEmbedded().getLocalDate() )
									)
									.objectField( "customBridgeOnProperty", b3 -> b3
											.field( "text", entity2.getEmbedded().getEmbedded().getText() )
											.field( "date", entity2.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "myEmbedded", b3 -> b3
											.field( "myTextField", entity2.getEmbedded().getEmbedded().getText() )
											.field( "myLocalDateField", entity2.getEmbedded().getEmbedded().getLocalDate() )
											.objectField( "customBridgeOnClass", b4 -> b4
													.field( "date", entity2.getEmbedded().getEmbedded().getLocalDate() )
											)
											.objectField( "customBridgeOnProperty", b4 -> b4
													.field( "text", entity2.getEmbedded().getEmbedded().getEmbedded().getText() )
													.field( "date", entity2.getEmbedded().getEmbedded().getEmbedded().getLocalDate() )
											)
									)
							)
					)
					.add( "3", b -> b
							.field( "myLocalDateField", entity3.getLocalDate() )
							.field( "myTextField", entity3.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity3.getText() )
									.field( "date", entity3.getLocalDate() )
							)
							.objectField( "customBridgeOnProperty", b2 -> b2
									.field( "text", entity3.getEmbedded().getText() )
									.field( "date", entity3.getEmbedded().getLocalDate() )
							)
							.objectField( "myEmbedded", b2 -> b2
									.field( "myTextField", entity3.getEmbedded().getText() )
									.field( "myLocalDateField", entity3.getEmbedded().getLocalDate() )
									.objectField( "customBridgeOnClass", b3 -> b3
											.field( "date", entity3.getEmbedded().getLocalDate() )
									)
									.objectField( "customBridgeOnProperty", b3 -> b3
											.field( "text", entity3.getEmbedded().getEmbedded().getText() )
											.field( "date", entity3.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "myEmbedded", b3 -> b3
											.field( "myTextField", entity3.getEmbedded().getEmbedded().getText() )
											.field( "myLocalDateField", entity3.getEmbedded().getEmbedded().getLocalDate() )
											.objectField( "customBridgeOnClass", b4 -> b4
													.field( "date", entity3.getEmbedded().getEmbedded().getLocalDate() )
											)
											.objectField( "customBridgeOnProperty", b4 -> b4
													.field( "text", entity3.getEmbedded().getEmbedded().getEmbedded().getText() )
													.field( "date", entity3.getEmbedded().getEmbedded().getEmbedded().getLocalDate() )
											)
									)
							)
					)
					.add( "6", b -> b
							.field( "myLocalDateField", entity6.getLocalDate() )
							.field( "myTextField", entity6.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity6.getText() )
									.field( "date", entity6.getLocalDate() )
							)
					);
		}
	}

	public static class ParentIndexedEntity {

		private LocalDate localDate;

		private IndexedEntity embedded;

		@GenericField(name = "myLocalDateField")
		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		@CustomPropertyBinding(objectName = "customBridgeOnProperty")
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(propertyName = "embeddingAsSingle")
				)
		)
		public IndexedEntity getEmbedded() {
			return embedded;
		}

		public void setEmbedded(IndexedEntity embedded) {
			this.embedded = embedded;
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	@CustomTypeBinding(objectName = "customBridgeOnClass")
	public static final class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private String text;

		private List<ParentIndexedEntity> embeddingAsSingle = new ArrayList<>();

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField(name = "myTextField")
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Override
		@IndexedEmbedded(name = "myEmbedded", includeDepth = 2,
				excludePaths = { "customBridgeOnClass.text", "myEmbedded.customBridgeOnClass.text" })
		public IndexedEntity getEmbedded() {
			return super.getEmbedded();
		}

		public List<ParentIndexedEntity> getEmbeddingAsSingle() {
			return embeddingAsSingle;
		}

	}

}
