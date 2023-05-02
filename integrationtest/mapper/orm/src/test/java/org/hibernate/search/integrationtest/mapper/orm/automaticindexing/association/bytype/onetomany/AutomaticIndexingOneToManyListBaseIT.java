/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.onetomany;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingMultiValuedAssociationBaseIT;
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
 * with a {@code @OneToMany List} association owned by the contained side.
 */
public class AutomaticIndexingOneToManyListBaseIT
		extends AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
						AutomaticIndexingOneToManyListBaseIT.IndexedEntity,
						AutomaticIndexingOneToManyListBaseIT.ContainingEntity,
						AutomaticIndexingOneToManyListBaseIT.ContainingEmbeddable,
						AutomaticIndexingOneToManyListBaseIT.ContainedEntity,
						AutomaticIndexingOneToManyListBaseIT.ContainedEmbeddable,
						List<AutomaticIndexingOneToManyListBaseIT.ContainedEntity>
		> {

	public AutomaticIndexingOneToManyListBaseIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainingEmbeddable.PRIMITIVES,
				ContainedEntity.PRIMITIVES, ContainedEmbeddable.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationMultiValuedOnContainedSide() {
		return false;
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return true;
	}

	@Override
	public void setup(OrmSetupHelper.SetupContext setupContext,
			ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		super.setup( setupContext, dataClearConfig );

		// We're simulating a mappedBy with two associations (see comments in annotation mapping),
		// so we need to clear one side before we can delete entities.
		dataClearConfig.preClear( ContainingEntity.class, containing -> {
			containing.getContainedElementCollectionAssociationsIndexedEmbedded().clear();
			containing.getContainedElementCollectionAssociationsNonIndexedEmbedded().clear();
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
				"containedElementCollectionAssociationsIndexedEmbedded.indexedField",
				"containedElementCollectionAssociationsIndexedEmbedded.indexedElementCollectionField",
				"containedElementCollectionAssociationsIndexedEmbedded.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@OneToMany(mappedBy = "containingAsIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private List<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containingAsNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedNonIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containingAsIndexedEmbeddedShallowReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private List<ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containingAsIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private List<ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containingAsUsedInCrossEntityDerivedProperty")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@OneToMany(mappedBy = "containingAsIndexedEmbeddedWithCast", targetEntity = ContainedEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = "indexedField", targetType = ContainedEntity.class)
		private List<Object> containedIndexedEmbeddedWithCast = new ArrayList<>();

		@IndexedEmbedded
		@Embedded
		private ContainingEmbeddable embeddedAssociations;

		/*
		 * No mappedBy here. The inverse side of associations within an element collection cannot use mappedBy.
		 * If they do, Hibernate ORM will fail (throw an exception) while attempting to walk down the mappedBy path,
		 * because it assumes the prefix of that path is an embeddable,
		 * and in this case it is a List.
		 * TODO use mappedBy when the above gets fixed in Hibernate ORM
		 */
		@OneToMany
		@OrderColumn(name = "idx")
		@JoinTable(name = "i_containedECAssocIdxEmb",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained"))
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "containingAsIndexedEmbedded")
		}))
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private List<ContainedEntity> containedElementCollectionAssociationsIndexedEmbedded;

		/*
		 * No mappedBy here. Same reason as just above.
		 * TODO use mappedBy when the above gets fixed in Hibernate ORM
		 */
		@OneToMany
		@OrderColumn(name = "idx")
		@JoinTable(name = "i_containedECAssocNonIdxEmb",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained"))
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "containingAsNonIndexedEmbedded")
		}))
		private List<ContainedEntity> containedElementCollectionAssociationsNonIndexedEmbedded;

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

		public List<ContainedEntity> getContainedElementCollectionAssociationsIndexedEmbedded() {
			return containedElementCollectionAssociationsIndexedEmbedded;
		}

		public List<ContainedEntity> getContainedElementCollectionAssociationsNonIndexedEmbedded() {
			return containedElementCollectionAssociationsNonIndexedEmbedded;
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
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedElementCollectionAssociationsIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedElementCollectionAssociationsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedElementCollectionAssociationsNonIndexedEmbedded() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainingEntity::getContainedElementCollectionAssociationsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
			}
		};
	}


	public static class ContainingEmbeddable {

		@OneToMany(mappedBy = "embeddedAssociations.containingAsIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private List<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "embeddedAssociations.containingAsNonIndexedEmbedded")
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

		@ManyToOne
		private ContainingEntity containingAsIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "containedNonIndexedEmbedded")
		private ContainingEntity containingAsNonIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "indexedEmbeddedShallow")
		private ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "indexedEmbeddedNoReindex")
		private ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "containedCrossEntityDP")
		private ContainingEntity containingAsUsedInCrossEntityDerivedProperty;

		@ManyToOne(targetEntity = ContainingEntity.class)
		@JoinColumn(name = "containedIndexedEmbeddedCast")
		private Object containingAsIndexedEmbeddedWithCast;

		@Embedded
		private ContainedEmbeddable embeddedAssociations;

		@ElementCollection
		@Embedded
		@OrderColumn(name = "idx")
		@CollectionTable(name = "c_ECAssoc")
		private List<ContainedEmbeddable> elementCollectionAssociations = new ArrayList<>();

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

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public ContainingEntity getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public void setContainingAsNonIndexedEmbedded(ContainingEntity containingAsNonIndexedEmbedded) {
			this.containingAsNonIndexedEmbedded = containingAsNonIndexedEmbedded;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedShallowReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate) {
			this.containingAsIndexedEmbeddedShallowReindexOnUpdate = containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedNoReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate) {
			this.containingAsIndexedEmbeddedNoReindexOnUpdate = containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainingEntity getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public void setContainingAsUsedInCrossEntityDerivedProperty(
				ContainingEntity containingAsUsedInCrossEntityDerivedProperty) {
			this.containingAsUsedInCrossEntityDerivedProperty = containingAsUsedInCrossEntityDerivedProperty;
		}

		public Object getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public void setContainingAsIndexedEmbeddedWithCast(Object containingAsIndexedEmbeddedWithCast) {
			this.containingAsIndexedEmbeddedWithCast = containingAsIndexedEmbeddedWithCast;
		}

		public ContainedEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainedEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public List<ContainedEmbeddable> getElementCollectionAssociations() {
			return elementCollectionAssociations;
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
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbedded,
						ContainedEntity::getContainingAsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsNonIndexedEmbedded,
						ContainedEntity::getContainingAsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedShallowReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedNoReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsUsedInCrossEntityDerivedProperty,
						ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainedEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainedEntity::setEmbeddedAssociations, ContainedEntity::getEmbeddedAssociations );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainedEmbeddable, List<ContainedEmbeddable>> elementCollectionAssociations() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getElementCollectionAssociations );
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

		@ManyToOne
		@JoinColumn(name = "emb_containedIdxEmbedded")
		private ContainingEntity containingAsIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "emb_containedNonIdxEmbedded")
		private ContainingEntity containingAsNonIndexedEmbedded;

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public ContainingEntity getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public void setContainingAsNonIndexedEmbedded(ContainingEntity containingAsNonIndexedEmbedded) {
			this.containingAsNonIndexedEmbedded = containingAsNonIndexedEmbedded;
		}

		static ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity> PRIMITIVES = new ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity>() {
			@Override
			public ContainedEmbeddable newInstance() {
				return new ContainedEmbeddable();
			}

			@Override
			public PropertyAccessor<ContainedEmbeddable, ContainingEntity> containingAsIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEmbeddable::setContainingAsIndexedEmbedded,
						ContainedEmbeddable::getContainingAsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEmbeddable, ContainingEntity> containingAsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEmbeddable::setContainingAsNonIndexedEmbedded,
						ContainedEmbeddable::getContainingAsNonIndexedEmbedded );
			}
		};
	}
}
