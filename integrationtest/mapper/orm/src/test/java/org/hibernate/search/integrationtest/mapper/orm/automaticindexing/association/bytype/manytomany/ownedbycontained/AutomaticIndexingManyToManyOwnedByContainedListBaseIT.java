/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytomany.ownedbycontained;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingMultiValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToMany List} association owned by the contained side.
 */
public class AutomaticIndexingManyToManyOwnedByContainedListBaseIT
		extends AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
						AutomaticIndexingManyToManyOwnedByContainedListBaseIT.IndexedEntity,
						AutomaticIndexingManyToManyOwnedByContainedListBaseIT.ContainingEntity,
						AutomaticIndexingManyToManyOwnedByContainedListBaseIT.ContainingEmbeddable,
						AutomaticIndexingManyToManyOwnedByContainedListBaseIT.ContainedEntity,
						AutomaticIndexingManyToManyOwnedByContainedListBaseIT.ContainedEmbeddable,
						List<AutomaticIndexingManyToManyOwnedByContainedListBaseIT.ContainedEntity>
		> {

	public AutomaticIndexingManyToManyOwnedByContainedListBaseIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainingEmbeddable.PRIMITIVES,
				ContainedEntity.PRIMITIVES, ContainedEmbeddable.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationMultiValuedOnContainedSide() {
		return true;
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return true;
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
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ManyToMany(mappedBy = "containingAsIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private List<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		@ManyToMany(mappedBy = "containingAsNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedNonIndexedEmbedded = new ArrayList<>();

		@ManyToMany(mappedBy = "containingAsIndexedEmbeddedShallowReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private List<ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@ManyToMany(mappedBy = "containingAsIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private List<ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@ManyToMany(mappedBy = "containingAsUsedInCrossEntityDerivedProperty")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@ManyToMany(mappedBy = "containingAsIndexedEmbeddedWithCast", targetEntity = ContainedEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = "indexedField", targetType = ContainedEntity.class)
		private List<Object> containedIndexedEmbeddedWithCast = new ArrayList<>();

		@IndexedEmbedded
		@Embedded
		private ContainingEmbeddable embeddedAssociations;

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

		public List<ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(List<ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public List<ContainedEntity> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(List<ContainedEntity> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public List<ContainedEntity> getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				List<ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public List<ContainedEntity> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				List<ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public List<ContainedEntity> getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public void setContainedUsedInCrossEntityDerivedProperty(
				List<ContainedEntity> containedUsedInCrossEntityDerivedProperty) {
			this.containedUsedInCrossEntityDerivedProperty = containedUsedInCrossEntityDerivedProperty;
		}

		public List<Object> getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public ContainingEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainingEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
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
			return computeDerived(
					containedUsedInCrossEntityDerivedProperty.stream().flatMap( c -> Stream.of(
							c.getFieldUsedInCrossEntityDerivedField1(),
							c.getFieldUsedInCrossEntityDerivedField2()
					) )
			);
		}

		static final ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity, List<ContainedEntity>> PRIMITIVES = new ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity, List<ContainedEntity>>() {
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
			public List<ContainedEntity> newContainedAssociation(List<ContainedEntity> original) {
				return new ArrayList<>( original );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedIndexedEmbedded, ContainingEntity::setContainedIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedNonIndexedEmbedded, ContainingEntity::setContainedNonIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedIndexedEmbeddedShallowReindexOnUpdate() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate,
						ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedIndexedEmbeddedNoReindexOnUpdate() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate,
						ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedUsedInCrossEntityDerivedProperty() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedUsedInCrossEntityDerivedProperty,
						ContainingEntity::setContainedUsedInCrossEntityDerivedProperty );
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, List<ContainedEntity>> containedIndexedEmbeddedWithCast() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						root -> (List) root.getContainedIndexedEmbeddedWithCast() );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainingEntity::setEmbeddedAssociations, ContainingEntity::getEmbeddedAssociations );
			}

			@Override
			public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
			}
		};
	}

	public static class ContainingEmbeddable {

		@ManyToMany(mappedBy = "embeddedAssociations.containingAsIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private List<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		@ManyToMany(mappedBy = "embeddedAssociations.containingAsNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedNonIndexedEmbedded = new ArrayList<>();

		public List<ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(List<ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public List<ContainedEntity> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(List<ContainedEntity> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		static final ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity, List<ContainedEntity>> PRIMITIVES = new ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity, List<ContainedEntity>>() {
			@Override
			public ContainingEmbeddable newInstance() {
				return new ContainingEmbeddable();
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEmbeddable, ContainedEntity, List<ContainedEntity>> containedIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEmbeddable::getContainedIndexedEmbedded, ContainingEmbeddable::setContainedIndexedEmbedded );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainingEmbeddable, ContainedEntity, List<ContainedEntity>> containedNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEmbeddable::getContainedNonIndexedEmbedded, ContainingEmbeddable::setContainedNonIndexedEmbedded );
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

		@ManyToMany
		@JoinTable(name = "i_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "i_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "i_indexedEmbeddedShallow",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "i_indexedEmbeddedNoReindex",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "i_containedCrossEntityDP",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@ManyToMany(targetEntity = ContainingEntity.class)
		@JoinTable(name = "i_containedIndexedEmbeddedCast",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Embedded
		private ContainedEmbeddable embeddedAssociations;

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "contained_IElementCF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexed")
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

		@ManyToMany
		@JoinTable(name = "i_emb_containedIdxEmbedded",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "i_emb_containedNonIdxEmbedded",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderColumn(name = "idx") // Test list associations, not bags
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
