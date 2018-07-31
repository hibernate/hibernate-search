/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events when
 * {@link TypeBridge}s or {@link PropertyBridge}s are involved.
 */
public class OrmAutomaticIndexingBridgeIT {
	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ContainedEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "typeBridge", b2 -> b2
						.field( "directField", String.class )
						.objectField( "child", b3 -> b3
								.field( "includedInTypeBridge", String.class )
						)
				)
				.objectField( "propertyBridge", b2 -> b2
						.field( "includedInPropertyBridge", String.class )
				)
		);

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void directPersistUpdateDelete() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDirectField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", entity1.getDirectField() )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setDirectField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", entity1.getDirectField() )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.delete( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "1" )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_typeBridge() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInTypeBridge( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "initialValue" )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInTypeBridge( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "updatedValue" )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInTypeBridge( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_typeBridge() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInTypeBridge( "initialValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInTypeBridge( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "initialValue" )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInTypeBridge( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "updatedValue" )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the bridge
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInTypeBridge( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_propertyBridge() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInPropertyBridge( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", "initialValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInPropertyBridge( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", "updatedValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInPropertyBridge( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", null )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_propertyBridge() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInPropertyBridge( "initialValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInPropertyBridge( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", "initialValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInPropertyBridge( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
							.objectField( "propertyBridge", b2 -> b2
									.field( "includedInPropertyBridge", "updatedValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the bridge
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInPropertyBridge( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "containing")
	@ContainingEntityTypeBridgeAnnotation
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@ContainingEntityPropertyBridgeAnnotation
		private ContainingEntity child;

		@ManyToOne
		private ContainedEntity containedSingle;

		@Basic
		private String directField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public String getDirectField() {
			return directField;
		}

		public void setDirectField(String directField) {
			this.directField = directField;
		}

	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {
		static final String INDEX = "IndexedEntity";
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsSingle = new ArrayList<>();

		@Basic
		private String includedInTypeBridge;

		@Basic
		private String includedInPropertyBridge;

		@Basic
		private String excludedFromAll;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsSingle() {
			return containingAsSingle;
		}

		public String getIncludedInTypeBridge() {
			return includedInTypeBridge;
		}

		public void setIncludedInTypeBridge(String includedInTypeBridge) {
			this.includedInTypeBridge = includedInTypeBridge;
		}

		public String getIncludedInPropertyBridge() {
			return includedInPropertyBridge;
		}

		public void setIncludedInPropertyBridge(String includedInPropertyBridge) {
			this.includedInPropertyBridge = includedInPropertyBridge;
		}

		public String getExcludedFromAll() {
			return excludedFromAll;
		}

		public void setExcludedFromAll(String excludedFromAll) {
			this.excludedFromAll = excludedFromAll;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@TypeBridgeMapping(bridge = @TypeBridgeReference(type = ContainingEntityTypeBridge.class))
	public @interface ContainingEntityTypeBridgeAnnotation {
	}

	public static class ContainingEntityTypeBridge implements TypeBridge {

		private PojoModelElementAccessor<String> directFieldSourceAccessor;
		private PojoModelElementAccessor<String> includedInTypeBridgeSourceAccessor;
		private IndexObjectFieldAccessor typeBridgeObjectFieldAccessor;
		private IndexFieldAccessor<String> directFieldTargetAccessor;
		private IndexObjectFieldAccessor childObjectFieldAccessor;
		private IndexFieldAccessor<String> includedInTypeBridgeTargetAccessor;

		@Override
		public void bind(TypeBridgeBindingContext context) {
			PojoModelType bridgedElement = context.getBridgedElement();
			directFieldSourceAccessor = bridgedElement.property( "directField" )
					.createAccessor( String.class );
			includedInTypeBridgeSourceAccessor = bridgedElement.property( "child" )
					.property( "containedSingle" )
					.property( "includedInTypeBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField typeBridgeObjectField = context.getIndexSchemaElement().objectField( "typeBridge" );
			typeBridgeObjectFieldAccessor = typeBridgeObjectField.createAccessor();
			directFieldTargetAccessor = typeBridgeObjectField.field( "directField" ).asString().createAccessor();
			IndexSchemaObjectField childObjectField = typeBridgeObjectField.objectField( "child" );
			childObjectFieldAccessor = childObjectField.createAccessor();
			includedInTypeBridgeTargetAccessor = childObjectField.field( "includedInTypeBridge" )
					.asString().createAccessor();
		}

		@Override
		public void write(DocumentElement target, PojoElement source) {
			DocumentElement typeBridgeObjectField = typeBridgeObjectFieldAccessor.add( target );
			directFieldTargetAccessor.write( typeBridgeObjectField, directFieldSourceAccessor.read( source ) );
			DocumentElement childObjectField = childObjectFieldAccessor.add( typeBridgeObjectField );
			includedInTypeBridgeTargetAccessor.write(
					childObjectField, includedInTypeBridgeSourceAccessor.read( source )
			);
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@PropertyBridgeMapping(bridge = @PropertyBridgeReference(type = ContainingEntityPropertyBridge.class))
	public @interface ContainingEntityPropertyBridgeAnnotation {
	}

	public static class ContainingEntityPropertyBridge implements PropertyBridge {

		private PojoModelElementAccessor<String> includedInPropertyBridgeSourceAccessor;
		private IndexObjectFieldAccessor propertyBridgeObjectFieldAccessor;
		private IndexFieldAccessor<String> includedInPropertyBridgeTargetAccessor;

		@Override
		public void bind(PropertyBridgeBindingContext context) {
			includedInPropertyBridgeSourceAccessor = context.getBridgedElement().property( "containedSingle" )
					.property( "includedInPropertyBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField propertyBridgeObjectField = context.getIndexSchemaElement().objectField( "propertyBridge" );
			propertyBridgeObjectFieldAccessor = propertyBridgeObjectField.createAccessor();
			includedInPropertyBridgeTargetAccessor = propertyBridgeObjectField.field( "includedInPropertyBridge" )
					.asString().createAccessor();
		}

		@Override
		public void write(DocumentElement target, PojoElement source) {
			DocumentElement propertyBridgeObjectField = propertyBridgeObjectFieldAccessor.add( target );
			includedInPropertyBridgeTargetAccessor.write(
					propertyBridgeObjectField, includedInPropertyBridgeSourceAccessor.read( source )
			);
		}
	}
}
