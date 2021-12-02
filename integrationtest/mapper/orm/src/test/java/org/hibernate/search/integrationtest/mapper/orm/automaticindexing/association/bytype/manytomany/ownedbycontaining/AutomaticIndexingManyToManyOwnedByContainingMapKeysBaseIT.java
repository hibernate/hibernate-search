/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytomany.ownedbycontaining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingMultiValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.SingleValuedPropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;


/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToMany Map} association owned by the containing side
 * where associated entities are map keys.
 */
public class AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT
		extends AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
						AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.IndexedEntity,
						AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainingEntity,
						AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainedEntity,
						Map<AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainedEntity, String>
		> {

	public AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT() {
		super( new ModelPrimitivesImpl() );
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		dataClearConfig.preClear( ContainedEntity.class, contained -> {
			contained.getContainingAsIndexedEmbedded().clear();
			contained.getContainingAsNonIndexedEmbedded().clear();
			contained.getContainingAsIndexedEmbeddedShallowReindexOnUpdate().clear();
			contained.getContainingAsIndexedEmbeddedNoReindexOnUpdate().clear();
			contained.getContainingAsUsedInCrossEntityDerivedProperty().clear();
			contained.getContainingAsIndexedEmbeddedWithCast().clear();
		} );
	}

	private static class ModelPrimitivesImpl
			implements MultiValuedModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity,
												Map<ContainedEntity, String>> {

		private final ContainerPrimitives<Map<ContainedEntity, String>, ContainedEntity> MAP_KEYS_PRIMITIVES =
				ContainerPrimitives.mapKeys( containedEntity -> containedEntity.getIndexedField() + "_value" );

		@Override
		public String getIndexName() {
			return IndexedEntity.INDEX;
		}

		@Override
		public boolean isAssociationOwnedByContainedSide() {
			return false;
		}

		@Override
		public Class<IndexedEntity> getIndexedClass() {
			return IndexedEntity.class;
		}

		@Override
		public Class<ContainingEntity> getContainingClass() {
			return ContainingEntity.class;
		}

		@Override
		public Class<ContainedEntity> getContainedClass() {
			return ContainedEntity.class;
		}

		@Override
		public IndexedEntity newIndexed(int id) {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainingEntity newContaining(int id) {
			ContainingEntity entity = new ContainingEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainedEntity newContained(int id) {
			ContainedEntity entity = new ContainedEntity();
			entity.setId( id );
			return entity;
		}


		@Override
		public SingleValuedPropertyAccessor<ContainingEntity, String> containingEntityNonIndexedField() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setNonIndexedField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainingEntity, ContainingEntity> child() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setChild );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainingEntity, ContainingEntity> parent() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setParent );
		}

		@Override
		public Map<ContainedEntity, String> newContainedAssociation(Map<ContainedEntity, String> original) {
			return new LinkedHashMap<>( original );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbedded, ContainingEntity::setContainedIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedNonIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					ContainingEntity::getContainedNonIndexedEmbedded, ContainingEntity::setContainedNonIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsNonIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsNonIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedIndexedEmbeddedShallowReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate,
					ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedIndexedEmbeddedNoReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate,
					ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedNoReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedUsedInCrossEntityDerivedProperty() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsUsedInCrossEntityDerivedProperty() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, Map<ContainedEntity, String>> containedIndexedEmbeddedWithCast() {
			return new MultiValuedPropertyAccessor<>( MAP_KEYS_PRIMITIVES,
					root -> (Map) root.getContainedIndexedEmbeddedWithCast() );
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedWithCast() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					root -> (List) root.getContainingAsIndexedEmbeddedWithCast() );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> indexedField() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setIndexedField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> nonIndexedField() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setNonIndexedField );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getIndexedElementCollectionField,
					ContainedEntity::setIndexedElementCollectionField );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> nonIndexedElementCollectionField() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getNonIndexedElementCollectionField,
					ContainedEntity::setNonIndexedElementCollectionField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInContainedDerivedField1 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInContainedDerivedField2 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
		}
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
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ElementCollection
		@JoinTable(
				name = "i_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_indexedEmbeddedShallow",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.SHALLOW,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbeddedShallowReindexOnUpdate = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_indexedEmbeddedNoReindex",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.NO,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedCrossEntityDP",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedUsedInCrossEntityDerivedProperty = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedIndexedEmbeddedCast",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyClass(ContainedEntity.class)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "value")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY),
				targetType = ContainedEntity.class
		)
		private Map<Object, String> containedIndexedEmbeddedWithCast = new LinkedHashMap<>();

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

		public Map<ContainedEntity, String> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Map<ContainedEntity, String> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(Map<ContainedEntity, String> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				Map<ContainedEntity, String> containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public Map<ContainedEntity, String> getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public Map<Object, String> getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return AbstractAutomaticIndexingAssociationBaseIT.computeDerived(
					containedUsedInCrossEntityDerivedProperty.keySet().stream().flatMap( c -> Stream.of(
							c.getFieldUsedInCrossEntityDerivedField1(),
							c.getFieldUsedInCrossEntityDerivedField2()
					) )
			);
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

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbedded",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_nonIndEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmdShallow",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbeddedShallowReindexOnUpdate",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmdNoReindex",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbeddedNoReindexOnUpdate",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_usedInCEDP",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany(targetEntity = ContainingEntity.class)
		@JoinTable(name = "contained_indEmdWithCast",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbeddedWithCast",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@CollectionTable(name = "contained_IElementCF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
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
			return AbstractAutomaticIndexingAssociationBaseIT.computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}
	}

}
