/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyClass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.common.impl.Closer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmBootstrapIntrospectorAccessTypeTest {

	@Parameterized.Parameters(name = "PropertyHandleFactoryName = {0}")
	public static List<Object[]> data() {
		return Arrays.asList( new Object[][] {
				{ null },
				{ "method-handle" },
				{ "java-lang-reflect" }
		} );
	}

	private final List<AutoCloseable> toClose = new ArrayList<>();

	private final String propertyHandleFactoryName;

	public HibernateOrmBootstrapIntrospectorAccessTypeTest(String propertyHandleFactoryName) {
		this.propertyHandleFactoryName = propertyHandleFactoryName;
	}

	@After
	public void cleanup() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.pushAll( AutoCloseable::close, toClose );
		}
	}

	@Test
	public void entity_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithDefaultFieldAccess.class );
		testEntityWithDefaultFieldAccess( introspector );
	}

	@Test
	public void entity_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithDefaultMethodAccess.class );
		testEntityWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddedId_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithEmbeddedIdWithDefaultFieldAccess.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddedId_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithEmbeddedIdWithDefaultMethodAccess.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embedded_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithEmbedded.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embedded_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithEmbedded.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddableElementCollection_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollection.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddableElementCollection_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollection.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddableAssociationMapKey_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableAssociationMapKey.class, OtherEntity.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddableAssociationMapKey_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableAssociationMapKey.class, OtherEntity.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddableAssociationMapValue_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableAssociationMapValue.class, OtherEntity.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddableAssociationMapValue_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableAssociationMapValue.class, OtherEntity.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddableElementCollectionMapKey_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollectionMapKey.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddableElementCollectionMapKey_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollectionMapKey.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	@Test
	public void embeddableElementCollectionMapValue_defaultFieldAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollectionMapValue.class );
		testEmbeddableWithDefaultFieldAccess( introspector );
	}

	@Test
	public void embeddableElementCollectionMapValue_defaultMethodAccess() {
		HibernateOrmBootstrapIntrospector introspector =
				createIntrospector( EntityWithEmbeddableElementCollectionMapValue.class );
		testEmbeddableWithDefaultMethodAccess( introspector );
	}

	private void testEntityWithDefaultFieldAccess(HibernateOrmBootstrapIntrospector introspector) {
		EntityWithDefaultFieldAccess entity = new EntityWithDefaultFieldAccess();
		PojoRawTypeModel<EntityWithDefaultFieldAccess> typeModel =
				introspector.getTypeModel( EntityWithDefaultFieldAccess.class );

		/*
		 * Try to get the value from each handle.
		 * If they return the correct value, we know they work correctly,
		 * because the types are designed in such a way that any other access type wouldn't work.
		 */
		PropertyHandle<?> propertyHandle = typeModel.getProperty( "propertyWithDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.propertyWithDefaultFieldAccess );
		propertyHandle = typeModel.getProperty( "idWithDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.idWithDefaultFieldAccess );
		propertyHandle = typeModel.getProperty( "propertyWithNonDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.getPropertyWithNonDefaultMethodAccess() );
	}

	private void testEntityWithDefaultMethodAccess(HibernateOrmBootstrapIntrospector introspector) {
		EntityWithDefaultMethodAccess entity = new EntityWithDefaultMethodAccess();
		PojoRawTypeModel<EntityWithDefaultMethodAccess> typeModel =
				introspector.getTypeModel( EntityWithDefaultMethodAccess.class );

		/*
		 * Try to get the value from each handle.
		 * If they return the correct value, we know they work correctly,
		 * because the types are designed in such a way that any other access type wouldn't work.
		 */
		PropertyHandle<?> propertyHandle = typeModel.getProperty( "propertyWithDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.getPropertyWithDefaultMethodAccess() );
		propertyHandle = typeModel.getProperty( "idWithDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.getIdWithDefaultMethodAccess() );
		propertyHandle = typeModel.getProperty( "propertyWithNonDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( entity ) ).isEqualTo( entity.propertyWithNonDefaultFieldAccess );
	}

	private void testEmbeddableWithDefaultFieldAccess(HibernateOrmBootstrapIntrospector introspector) {
		EmbeddableWithDefaultFieldAccess embeddable = new EmbeddableWithDefaultFieldAccess();
		PojoRawTypeModel<EmbeddableWithDefaultFieldAccess> typeModel =
				introspector.getTypeModel( EmbeddableWithDefaultFieldAccess.class );

		/*
		 * Try to get the value from each handle.
		 * If they return the correct value, we know they work correctly,
		 * because the types are designed in such a way that any other access type wouldn't work.
		 */
		PropertyHandle<?> propertyHandle = typeModel.getProperty( "propertyWithDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( embeddable ) ).isEqualTo( embeddable.propertyWithDefaultFieldAccess );
		propertyHandle = typeModel.getProperty( "propertyWithNonDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( embeddable ) ).isEqualTo( embeddable.getPropertyWithNonDefaultMethodAccess() );

		/*
		 * Also test that the access type of nested embeddeds is correctly detected.
		 */
		NestedEmbeddableWithDefaultFieldAccess nestedEmbeddable = new NestedEmbeddableWithDefaultFieldAccess();
		PojoRawTypeModel<NestedEmbeddableWithDefaultFieldAccess> nestedEmbeddableTypeModel =
				introspector.getTypeModel( NestedEmbeddableWithDefaultFieldAccess.class );
		propertyHandle = nestedEmbeddableTypeModel.getProperty( "propertyWithDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( nestedEmbeddable ) ).isEqualTo( nestedEmbeddable.propertyWithDefaultFieldAccess );
		propertyHandle = nestedEmbeddableTypeModel.getProperty( "propertyWithNonDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( nestedEmbeddable ) ).isEqualTo( nestedEmbeddable.getPropertyWithNonDefaultMethodAccess() );
	}

	private void testEmbeddableWithDefaultMethodAccess(HibernateOrmBootstrapIntrospector introspector) {
		EmbeddableWithDefaultMethodAccess embeddable = new EmbeddableWithDefaultMethodAccess();
		PojoRawTypeModel<EmbeddableWithDefaultMethodAccess> typeModel =
				introspector.getTypeModel( EmbeddableWithDefaultMethodAccess.class );

		/*
		 * Try to get the value from each handle.
		 * If they return the correct value, we know they work correctly,
		 * because the types are designed in such a way that any other access type wouldn't work.
		 */
		PropertyHandle<?> propertyHandle = typeModel.getProperty( "propertyWithDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( embeddable ) ).isEqualTo( embeddable.getPropertyWithDefaultMethodAccess() );
		propertyHandle = typeModel.getProperty( "propertyWithNonDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( embeddable ) ).isEqualTo( embeddable.propertyWithNonDefaultFieldAccess );

		/*
		 * Also test that the access type of nested embeddeds is correctly detected.
		 */
		NestedEmbeddableWithDefaultMethodAccess nestedEmbeddable = new NestedEmbeddableWithDefaultMethodAccess();
		PojoRawTypeModel<NestedEmbeddableWithDefaultMethodAccess> nestedEmbeddableTypeModel =
				introspector.getTypeModel( NestedEmbeddableWithDefaultMethodAccess.class );
		propertyHandle = nestedEmbeddableTypeModel.getProperty( "propertyWithDefaultMethodAccess" ).getHandle();
		assertThat( propertyHandle.get( nestedEmbeddable ) ).isEqualTo( nestedEmbeddable.getPropertyWithDefaultMethodAccess() );
		propertyHandle = nestedEmbeddableTypeModel.getProperty( "propertyWithNonDefaultFieldAccess" ).getHandle();
		assertThat( propertyHandle.get( nestedEmbeddable ) ).isEqualTo( nestedEmbeddable.propertyWithNonDefaultFieldAccess );
	}

	private HibernateOrmBootstrapIntrospector createIntrospector(Class<?> ... entityClasses) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		// Some properties that are not relevant to our test, but necessary to create the Metadata
		registryBuilder.applySetting( AvailableSettings.DIALECT, H2Dialect.class );
		StandardServiceRegistry serviceRegistry = registryBuilder.build();
		toClose.add( serviceRegistry );

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		for ( Class<?> entityClass : entityClasses ) {
			metadataSources.addAnnotatedClass( entityClass );
		}
		Metadata metadata = metadataSources.buildMetadata();

		MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		ReflectionManager reflectionManager = metadataImplementor.getTypeConfiguration()
				.getMetadataBuildingContext().getBootstrapContext().getReflectionManager();

		Map<String, Object> properties = new HashMap<>();
		if ( propertyHandleFactoryName != null ) {
			properties.put(
					HibernateOrmMapperSpiSettings.Radicals.PROPERTY_HANDLE_FACTORY,
					propertyHandleFactoryName
			);
		}

		return HibernateOrmBootstrapIntrospector.create(
				metadata, reflectionManager,
				ConfigurationPropertySource.fromMap( properties )
		);
	}

	private static <T> T methodShouldNotBeCalled() {
		Assert.fail( "This method should not be called" );
		return null;
	}

	@Entity
	@Access(AccessType.FIELD)
	private static class EntityWithDefaultFieldAccess {
		@Id
		private String idWithDefaultFieldAccess = "idWithDefaultFieldAccessValue";

		@Basic
		protected String propertyWithDefaultFieldAccess = "propertyWithDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		public String getPropertyWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access(AccessType.PROPERTY)
		@Basic
		public String getPropertyWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Entity
	@Access(AccessType.PROPERTY)
	private static class EntityWithDefaultMethodAccess {
		private String idWithDefaultMethodAccess = "idWithDefaultMethodAccessValue";

		@Access( AccessType.FIELD )
		@Basic
		protected String propertyWithNonDefaultFieldAccess = "propertyWithNonDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		@Id
		public String getIdWithDefaultMethodAccess() {
			return idWithDefaultMethodAccess;
		}

		public void setIdWithDefaultMethodAccess(String idWithDefaultMethodAccess) {
			this.idWithDefaultMethodAccess = idWithDefaultMethodAccess;
		}

		public String getPropertyWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Basic
		public String getPropertyWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	/*
	 * IMPORTANT: In order to avoid false positives, and due to how the introspector works,
	 * this type must reference each embeddable type ONLY ONCE.
	 * Otherwise, if one reference to the embeddable type is not correctly processed,
	 * this error could be compensated by the other reference, which could be correctly processed.
	 */
	@Entity
	private static class EntityWithEmbedded {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableWithDefaultFieldAccess embeddedWithDefaultFieldAccess;

		@Embedded
		private EmbeddableWithDefaultMethodAccess embeddedWithDefaultMethodAccess;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddedIdWithDefaultFieldAccess {
		@EmbeddedId
		private EmbeddableWithDefaultFieldAccess embeddedId;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddedIdWithDefaultMethodAccess {
		@EmbeddedId
		private EmbeddableWithDefaultMethodAccess embeddedId;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddableElementCollection {
		@Id
		private Integer id;

		@ElementCollection
		private List<EmbeddableWithDefaultFieldAccess> withDefaultFieldAccess;

		@ElementCollection
		private List<EmbeddableWithDefaultMethodAccess> withDefaultMethodAccess;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddableAssociationMapKey {
		@Id
		private Integer id;

		@OneToMany
		private Map<EmbeddableWithDefaultFieldAccess, OtherEntity> withDefaultFieldAccess;

		@OneToMany
		private Map<EmbeddableWithDefaultMethodAccess, OtherEntity> withDefaultMethodAccess;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddableAssociationMapValue {
		@Id
		private Integer id;

		// Not sure this actually creates an association to OtherEntity, but this is the best I could come up with.
		@MapKeyClass(OtherEntity.class)
		@ElementCollection
		private Map<OtherEntity, EmbeddableWithDefaultFieldAccess> withDefaultFieldAccess;

		@MapKeyClass(OtherEntity.class)
		@ElementCollection
		private Map<OtherEntity, EmbeddableWithDefaultMethodAccess> withDefaultMethodAccess;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddableElementCollectionMapKey {
		@Id
		private Integer id;

		@ElementCollection
		private Map<EmbeddableWithDefaultFieldAccess, OtherEmbeddable> withDefaultFieldAccess;

		@ElementCollection
		private Map<EmbeddableWithDefaultMethodAccess, OtherEmbeddable> withDefaultMethodAccess;
	}

	/*
	 * IMPORTANT: See comment on EntityWithEmbedded.
	 */
	@Entity
	private static class EntityWithEmbeddableElementCollectionMapValue {
		@Id
		private Integer id;

		@ElementCollection
		private Map<OtherEmbeddable, EmbeddableWithDefaultFieldAccess> withDefaultFieldAccess;

		@ElementCollection
		private Map<OtherEmbeddable, EmbeddableWithDefaultMethodAccess> withDefaultMethodAccess;
	}

	@Embeddable
	@Access(AccessType.FIELD)
	private static class EmbeddableWithDefaultFieldAccess {
		@Basic
		protected String propertyWithDefaultFieldAccess = "propertyWithDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		@Embedded
		private NestedEmbeddableWithDefaultFieldAccess nestedEmbeddable;

		public String getPropertyWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access(AccessType.PROPERTY)
		@Basic
		public String getPropertyWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	private static class EmbeddableWithDefaultMethodAccess {
		@Access(AccessType.FIELD)
		@Basic
		protected String propertyWithNonDefaultFieldAccess = "propertyWithNonDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		private NestedEmbeddableWithDefaultMethodAccess nestedEmbeddable;

		public String getPropertyWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Basic
		public String getPropertyWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}

		@Embedded
		public NestedEmbeddableWithDefaultMethodAccess getNestedEmbeddable() {
			return nestedEmbeddable;
		}

		public void setNestedEmbeddable(
				NestedEmbeddableWithDefaultMethodAccess nestedEmbeddable) {
			this.nestedEmbeddable = nestedEmbeddable;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	private static class NestedEmbeddableWithDefaultFieldAccess {
		@Basic
		protected String propertyWithDefaultFieldAccess = "propertyWithDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		public String getPropertyWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access(AccessType.PROPERTY)
		@Basic
		public String getPropertyWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	private static class NestedEmbeddableWithDefaultMethodAccess {
		@Access(AccessType.FIELD)
		@Basic
		protected String propertyWithNonDefaultFieldAccess = "propertyWithNonDefaultFieldAccessValue";

		@Transient
		private String internalFieldWithDifferentName = "internalFieldWithDifferentNameValue";

		public String getPropertyWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setPropertyWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Basic
		public String getPropertyWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setPropertyWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Entity
	private static class OtherEntity {

		@Id
		private Integer id;

		@Basic
		private String otherEmbeddableProperty;

	}

	@Embeddable
	private static class OtherEmbeddable {

		@Basic
		private String otherEmbeddableProperty;

	}
}