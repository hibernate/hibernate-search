/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;

public class AbstractAutomaticIndexingFilterIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
						.field( "indexedField", String.class )
						.objectField(
								"containedIndexedEmbedded", b2 -> b2.field( "indexedField", String.class ).multiValued( true )
						)
				)
				.expectSchema( OtherIndexedEntity.INDEX, b -> b
						.field( "indexedField", String.class )
						.objectField(
								"containedIndexedEmbedded",
								b2 -> b2.field( "indexedField", String.class ).multiValued( true )
						)
				)
				.expectSchema( EntityA.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity2A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1B.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( EntityFromSuperclass.INDEX, b -> b.field( "indexedField", String.class ) );

		setupContext.withAnnotatedTypes( IndexedEntity.class, OtherIndexedEntity.class, ContainedEntity.class,
				EntityA.class, Entity1A.class, Entity1B.class, Entity2A.class, EntityFromSuperclass.class, SuperClass.class,
				SimpleNotIndexedEntity.class, NotIndexedEntityFromSuperclass.class
		);
	}

	@Before
	public void clearFilter() throws Exception {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> { /*clear out any settings from tests*/ }
		);
	}

	@Entity(name = IndexedEntity.INDEX)
	@Indexed
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		@Basic
		private String nonIndexedField;

		@OneToMany(mappedBy = "containingAsIndexedEmbedded")
		@IndexedEmbedded
		private Collection<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public Collection<ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Collection<ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}
	}

	@Entity(name = OtherIndexedEntity.INDEX)
	@Indexed
	public static class OtherIndexedEntity {

		static final String INDEX = "OtherIndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		@Basic
		private String nonIndexedField;

		@OneToMany(mappedBy = "otherContainingAsIndexedEmbedded")
		@IndexedEmbedded
		private Collection<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		public OtherIndexedEntity() {
		}

		public OtherIndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public Collection<ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Collection<ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@ManyToOne
		private IndexedEntity containingAsIndexedEmbedded;

		@ManyToOne
		private OtherIndexedEntity otherContainingAsIndexedEmbedded;

		@Basic
		@GenericField
		private String indexedField;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(IndexedEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public OtherIndexedEntity getOtherContainingAsIndexedEmbedded() {
			return otherContainingAsIndexedEmbedded;
		}

		public void setOtherContainingAsIndexedEmbedded(OtherIndexedEntity otherContainingAsIndexedEmbedded) {
			this.otherContainingAsIndexedEmbedded = otherContainingAsIndexedEmbedded;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	@Entity(name = EntityA.INDEX)
	@Indexed
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class EntityA {

		static final String INDEX = "A";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public EntityA() {
		}

		public EntityA(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	@Entity(name = Entity1A.INDEX)
	@Indexed
	public static class Entity1A extends EntityA {
		static final String INDEX = "A1";

		public Entity1A() {
		}

		public Entity1A(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity(name = Entity1B.INDEX)
	@Indexed
	public static class Entity1B extends EntityA {
		static final String INDEX = "B1";

		public Entity1B() {
		}

		public Entity1B(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity(name = Entity2A.INDEX)
	@Indexed
	public static class Entity2A extends Entity1A {
		static final String INDEX = "A2";

		public Entity2A() {
		}

		public Entity2A(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}


	@MappedSuperclass
	public static class SuperClass {
		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public SuperClass() {
		}

		public SuperClass(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}
	}

	public interface InterfaceA {
	}

	public interface InterfaceB {
	}

	@Entity(name = EntityFromSuperclass.INDEX)
	@Indexed
	public static class EntityFromSuperclass extends SuperClass implements InterfaceA, InterfaceB {
		static final String INDEX = "EntityFromSuperclass";

		public EntityFromSuperclass() {
		}

		public EntityFromSuperclass(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity(name = NotIndexedEntityFromSuperclass.INDEX)
	public static class NotIndexedEntityFromSuperclass extends SuperClass implements InterfaceA, InterfaceB {
		static final String INDEX = "NotIndexedEntityFromSuperclass";

		public NotIndexedEntityFromSuperclass() {
		}

		public NotIndexedEntityFromSuperclass(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity
	public static class SimpleNotIndexedEntity {
		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public SimpleNotIndexedEntity() {
		}

		public SimpleNotIndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}
	}

	public static class NotAnEntity {
	}

	@Indexed(index = IndexedNotAnEntity.INDEX)
	public static class IndexedNotAnEntity {
		static final String INDEX = "IndexedNotAnEntity";

		@DocumentId
		private Integer id;

		@GenericField
		private String indexedField;

		public IndexedNotAnEntity() {
		}

		public IndexedNotAnEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

}
