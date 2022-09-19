/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytoone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingSingleValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToOne} association owned by the containing side.
 */
public class AutomaticIndexingManyToOneBaseIT
		extends AbstractAutomaticIndexingSingleValuedAssociationBaseIT<
						AutomaticIndexingManyToOneBaseIT.IndexedEntity,
						AutomaticIndexingManyToOneBaseIT.ContainingEntity,
						AutomaticIndexingManyToOneBaseIT.ContainingEmbeddable,
						AutomaticIndexingManyToOneBaseIT.ContainedEntity,
						AutomaticIndexingManyToOneBaseIT.ContainedEmbeddable
				> {

	public AutomaticIndexingManyToOneBaseIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainingEmbeddable.PRIMITIVES,
				ContainedEntity.PRIMITIVES, ContainedEmbeddable.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationMultiValuedOnContainedSide() {
		return true;
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return false;
	}

	@Override
	protected boolean isAssociationLazyOnContainingSide() {
		return false;
	}

	@Override
	public void setup(OrmSetupHelper.SetupContext setupContext,
			ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		super.setup( setupContext, dataClearConfig );

		// We're simulating a mappedBy with two associations (see comments in annotation mapping),
		// so we need to clear one side before we can delete entities.
		dataClearConfig.preClear( ContainedEntity.class, contained -> {
			contained.getContainingAsElementCollectionAssociationsIndexedEmbedded().clear();
			contained.getContainingAsElementCollectionAssociationsNonIndexedEmbedded().clear();
		} );
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		private String nonIndexedField;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedWithCast.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedElementCollectionField",
				"embeddedAssociations.containedIndexedEmbedded.containedDerivedField",
				"elementCollectionAssociations.containedIndexedEmbedded.indexedField",
				"elementCollectionAssociations.containedIndexedEmbedded.indexedElementCollectionField",
				"elementCollectionAssociations.containedIndexedEmbedded.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ManyToOne
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedIndexedEmbedded;

		@ManyToOne
		private ContainedEntity containedNonIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "CIndexedEmbeddedSROU")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "CIndexedEmbeddedNROU")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "CCrossEntityDerived")
		private ContainedEntity containedUsedInCrossEntityDerivedProperty;

		@ManyToOne(targetEntity = ContainedEntity.class)
		@JoinColumn(name = "CIndexedEmbeddedCast")
		@IndexedEmbedded(includePaths = { "indexedField" }, targetType = ContainedEntity.class)
		private Object containedIndexedEmbeddedWithCast;

		@IndexedEmbedded
		@Embedded
		private ContainingEmbeddable embeddedAssociations;

		@IndexedEmbedded
		@ElementCollection
		@Embedded
		@OrderColumn(name = "idx")
		@CollectionTable(name = "i_ECAssoc")
		private List<ContainingEmbeddable> elementCollectionAssociations = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
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

		public ContainedEntity getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(ContainedEntity containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public ContainedEntity getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(ContainedEntity containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public ContainedEntity getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainedEntity getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainedEntity getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public void setContainedUsedInCrossEntityDerivedProperty(
				ContainedEntity containedUsedInCrossEntityDerivedProperty) {
			this.containedUsedInCrossEntityDerivedProperty = containedUsedInCrossEntityDerivedProperty;
		}

		public Object getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public void setContainedIndexedEmbeddedWithCast(Object containedIndexedEmbeddedWithCast) {
			this.containedIndexedEmbeddedWithCast = containedIndexedEmbeddedWithCast;
		}

		public ContainingEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainingEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public List<ContainingEmbeddable> getElementCollectionAssociations() {
			return elementCollectionAssociations;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return containedUsedInCrossEntityDerivedProperty == null
					? Optional.empty()
					: computeDerived( Stream.of(
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField1(),
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField2()
					) );
		}

		static final ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity> PRIMITIVES = new ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity>() {
			@Override
			public Class<ContainingEntity> entityClass() {
				return ContainingEntity.class;
			}

			@Override
			public ContainingEntity newInstance(int id) {
				ContainingEntity entity = new ContainingEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> child() {
				return PropertyAccessor.create( ContainingEntity::setChild );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> parent() {
				return PropertyAccessor.create( ContainingEntity::setParent );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbedded,
						ContainingEntity::getContainedIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedNonIndexedEmbedded,
						ContainingEntity::getContainedNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate );
			}


			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainingEntity::setContainedUsedInCrossEntityDerivedProperty,
						ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainingEntity::setEmbeddedAssociations, ContainingEntity::getEmbeddedAssociations );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainingEmbeddable, List<ContainingEmbeddable>> elementCollectionAssociations() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getElementCollectionAssociations );
			}

			@Override
			public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
			}
		};
	}

	public static class ContainingEmbeddable {

		@ManyToOne
		@JoinColumn(name = "CEmbIdxEmbedded")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				name = "containedIndexedEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainedEntity embContainedIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "CEmbNonIdxEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainedEntity embContainedNonIndexedEmbedded;

		public ContainedEntity getEmbContainedIndexedEmbedded() {
			return embContainedIndexedEmbedded;
		}

		public void setEmbContainedIndexedEmbedded(ContainedEntity embContainedIndexedEmbedded) {
			this.embContainedIndexedEmbedded = embContainedIndexedEmbedded;
		}

		public ContainedEntity getEmbContainedNonIndexedEmbedded() {
			return embContainedNonIndexedEmbedded;
		}

		public void setEmbContainedNonIndexedEmbedded(ContainedEntity embContainedNonIndexedEmbedded) {
			this.embContainedNonIndexedEmbedded = embContainedNonIndexedEmbedded;
		}

		static final ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity> PRIMITIVES = new ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity>() {
			@Override
			public ContainingEmbeddable newInstance() {
				return new ContainingEmbeddable();
			}

			@Override
			public PropertyAccessor<ContainingEmbeddable, ContainedEntity> containedIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEmbeddable::setEmbContainedIndexedEmbedded,
						ContainingEmbeddable::getEmbContainedIndexedEmbedded
				);
			}

			@Override
			public PropertyAccessor<ContainingEmbeddable, ContainedEntity> containedNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEmbeddable::setEmbContainedNonIndexedEmbedded,
						ContainingEmbeddable::getEmbContainedNonIndexedEmbedded
				);
			}
		};
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

		static final IndexedEntityPrimitives<IndexedEntity> PRIMITIVES = new IndexedEntityPrimitives<IndexedEntity>() {
			@Override
			public Class<IndexedEntity> entityClass() {
				return IndexedEntity.class;
			}

			@Override
			public String indexName() {
				return IndexedEntity.INDEX;
			}

			@Override
			public IndexedEntity newInstance(int id) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( id );
				return entity;
			}
		};
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containedNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedShallowReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containedUsedInCrossEntityDerivedProperty")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedWithCast", targetEntity = ContainingEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Embedded
		private ContainedEmbeddable embeddedAssociations;

		/*
		 * No mappedBy here. The inverse side of associations within an element collection cannot use mappedBy.
		 * If they do, Hibernate ORM will fail (throw an exception) while attempting to walk down the mappedBy path,
		 * because it assumes the prefix of that path is an embeddable,
		 * and in this case it is a List.
		 * Also, we're using @ManyToMany to prevent Hibernate ORM from adding a unique constraint
		 * on the foreign key to the "containing" column:
		 * while association is a @ManyToOne on the "containing" side,
		 * it can actually refer to multiple "contained" entities
		 * because the association is wrapped in an @ElementCollection
		 * (so you basically have multiple rows with a @ManyToOne column for the same entity);
		 * in short, @ElementCollection + @ManyToOne => @ManyToMany.
		 * TODO use @OneToMany(mappedBy = ...) when the above gets fixed in Hibernate ORM
		 */
		@ManyToMany
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@JoinTable(name = "c_containingAsECAssocIdxEmb",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "embContainedIndexedEmbedded")
		}))
		private List<ContainingEntity> containingAsElementCollectionAssociationsIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, and using @ManyToMany. Same reason as just above.
		 * TODO use @OneToMany(mappedBy = ...) when the above gets fixed in Hibernate ORM
		 */
		@ManyToMany
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@JoinTable(name = "c_containingAsECAssocNonIdxEmb",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "embContainedNonIndexedEmbedded")
		}))
		private List<ContainingEntity> containingAsElementCollectionAssociationsNonIndexedEmbedded = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "indexedECF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexedECF")
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF1")
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF2")
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF1")
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF2")
		private String fieldUsedInCrossEntityDerivedField2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public List<Object> getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public ContainedEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainedEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public List<ContainingEntity> getContainingAsElementCollectionAssociationsIndexedEmbedded() {
			return containingAsElementCollectionAssociationsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsElementCollectionAssociationsNonIndexedEmbedded() {
			return containingAsElementCollectionAssociationsNonIndexedEmbedded;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public String getFieldUsedInContainedDerivedField1() {
			return fieldUsedInContainedDerivedField1;
		}

		public void setFieldUsedInContainedDerivedField1(String fieldUsedInContainedDerivedField1) {
			this.fieldUsedInContainedDerivedField1 = fieldUsedInContainedDerivedField1;
		}

		public String getFieldUsedInContainedDerivedField2() {
			return fieldUsedInContainedDerivedField2;
		}

		public void setFieldUsedInContainedDerivedField2(String fieldUsedInContainedDerivedField2) {
			this.fieldUsedInContainedDerivedField2 = fieldUsedInContainedDerivedField2;
		}

		public String getFieldUsedInCrossEntityDerivedField1() {
			return fieldUsedInCrossEntityDerivedField1;
		}

		public void setFieldUsedInCrossEntityDerivedField1(String fieldUsedInCrossEntityDerivedField1) {
			this.fieldUsedInCrossEntityDerivedField1 = fieldUsedInCrossEntityDerivedField1;
		}

		public String getFieldUsedInCrossEntityDerivedField2() {
			return fieldUsedInCrossEntityDerivedField2;
		}

		public void setFieldUsedInCrossEntityDerivedField2(String fieldUsedInCrossEntityDerivedField2) {
			this.fieldUsedInCrossEntityDerivedField2 = fieldUsedInCrossEntityDerivedField2;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField1")),
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField2"))
		})
		public Optional<String> getContainedDerivedField() {
			return computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}

		static ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity> PRIMITIVES = new ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity>() {
			@Override
			public Class<ContainedEntity> entityClass() {
				return ContainedEntity.class;
			}

			@Override
			public ContainedEntity newInstance(int id) {
				ContainedEntity entity = new ContainedEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsNonIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedNoReindexOnUpdate() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsUsedInCrossEntityDerivedProperty() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedWithCast() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						root -> (List) root.getContainingAsIndexedEmbeddedWithCast() );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainedEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainedEntity::setEmbeddedAssociations, ContainedEntity::getEmbeddedAssociations );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsElementCollectionAssociationsIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsElementCollectionAssociationsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsElementCollectionAssociationsNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getContainingAsElementCollectionAssociationsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> indexedField() {
				return PropertyAccessor.create( ContainedEntity::setIndexedField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainedEntity::setNonIndexedField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getIndexedElementCollectionField,
						ContainedEntity::setIndexedElementCollectionField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> nonIndexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getNonIndexedElementCollectionField,
						ContainedEntity::setNonIndexedElementCollectionField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField2 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
			}
		};
	}

	public static class ContainedEmbeddable {

		@OneToMany(mappedBy = "embeddedAssociations.embContainedIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "embeddedAssociations.embContainedNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		public List<ContainingEntity> getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		static ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity> PRIMITIVES = new ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity>() {
			@Override
			public ContainedEmbeddable newInstance() {
				return new ContainedEmbeddable();
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEmbeddable, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEmbeddable::getContainingAsIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEmbeddable, ContainingEntity, List<ContainingEntity>> containingAsNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEmbeddable::getContainingAsNonIndexedEmbedded );
			}
		};
	}

}
