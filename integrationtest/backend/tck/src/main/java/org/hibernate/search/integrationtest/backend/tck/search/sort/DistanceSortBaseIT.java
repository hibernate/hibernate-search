/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class DistanceSortBaseIT {
	//CHECKSTYLE:ON

	private static final GeoPointFieldTypeDescriptor supportedFieldType;
	private static final List<StandardFieldTypeDescriptor<GeoPoint>> supportedFieldTypes =
			new ArrayList<>();
	private static final List<FieldTypeDescriptor<?, ?>> unsupportedFieldTypes =
			new ArrayList<>();
	static {
		supportedFieldType = GeoPointFieldTypeDescriptor.INSTANCE;
		supportedFieldTypes.add( supportedFieldType );
		for ( FieldTypeDescriptor<?, ?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( !supportedFieldType.equals( fieldType ) ) {
				unsupportedFieldTypes.add( fieldType );
			}
		}
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( InvalidFieldConfigured.index, UnsupportedTypeConfigured.index,
						SortableConfigured.sortableDefaultIndex, SortableConfigured.sortableYesIndex,
						SortableConfigured.sortableNoIndex )
				.setup();
	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InvalidFieldConfigured extends AbstractSortInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldConfigured() {
			super( index );
		}

		@Override
		protected void trySort(SearchSortFactory f, String fieldPath) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String sortTrait() {
			return "sort:distance";
		}
	}

	@Nested
	class UnsupportedTypeIT extends UnsupportedTypeConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class UnsupportedTypeConfigured extends AbstractSortUnsupportedTypesIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, unsupportedFieldTypes ) )
						.name( "unsupportedType" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : unsupportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void trySort(SearchSortFactory f, String fieldPath) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String sortTrait() {
			return "sort:distance";
		}
	}

	@Nested
	class SortableIT extends SortableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SortableConfigured extends AbstractSortSortableIT {
		private static final SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex =
				SimpleMappedIndex.of( root -> new SortableDefaultIndexBinding( root, supportedFieldTypes ) )
						.name( "sortableDefault" );
		private static final SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex =
				SimpleMappedIndex.of( root -> new SortableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "sortableYes" );

		private static final SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex =
				SimpleMappedIndex.of( root -> new SortableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "sortableNo" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		void sortable_default_trait(SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
				SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
				SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			if ( TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault() ) {
				// On Elasticsearch GeoPoint fields are always sortable by distance when projectable
				String fieldPath = sortableDefaultIndex.binding().field.get( fieldType ).relativeFieldName;

				assertThat( sortableDefaultIndex.toApi().descriptor().field( fieldPath ) )
						.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
								.as( "traits of field '" + fieldPath + "'" )
								.contains( sortTrait() ) );
			}
			else {
				super.sortable_default_trait( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType );
			}
		}

		@Override
		void sortable_no_trait(SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
				SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
				SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			if ( TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault() ) {
				// On Elasticsearch GeoPoint fields are always sortable by distance when projectable
				String fieldPath = sortableNoIndex.binding().field.get( fieldType ).relativeFieldName;

				assertThat( sortableNoIndex.toApi().descriptor().field( fieldPath ) )
						.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
								.as( "traits of field '" + fieldPath + "'" )
								.contains( sortTrait() ) );
			}
			else {
				super.sortable_no_trait( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType );
			}
		}

		@Override
		void sortable_default_use(SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
				SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
				SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			assumeFalse(
					TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault(),
					"Skipping test for ES GeoPoint as those would become sortable by default in this case."
			);
			super.sortable_default_use( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType );
		}

		@Override
		void sortable_no_use(SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
				SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
				SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			assumeFalse(
					TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault(),
					"Skipping test for ES GeoPoint as those would become sortable by default in this case."
			);
			super.sortable_no_use( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType );
		}

		@Override
		void multiIndex_incompatibleSortable(SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
				SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
				SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			assumeFalse(
					TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault(),
					"Skipping test for ES GeoPoint as those would become sortable by default in this case."
			);
			super.multiIndex_incompatibleSortable( sortableDefaultIndex, sortableYesIndex, sortableNoIndex, fieldType );
		}

		@Override
		protected void trySort(SearchSortFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String sortTrait() {
			return "sort:distance";
		}
	}
}
