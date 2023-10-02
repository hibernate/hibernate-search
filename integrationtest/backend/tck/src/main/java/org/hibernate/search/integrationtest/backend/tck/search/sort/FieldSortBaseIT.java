/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class FieldSortBaseIT {
	//CHECKSTYLE:ON

	private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<FieldTypeDescriptor<?, ?>> unsupportedFieldTypes = new ArrayList<>();

	static {
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
			}
			else {
				unsupportedFieldTypes.add( fieldType );
			}
		}
		unsupportedFieldTypes.addAll( FieldTypeDescriptor.getAllNonStandard() );
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
			f.field( fieldPath );
		}

		@Override
		protected String sortTrait() {
			return "sort:field";
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
			f.field( fieldPath );
		}

		@Override
		protected String sortTrait() {
			return "sort:field";
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
		protected void trySort(SearchSortFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.field( fieldPath );
		}

		@Override
		protected String sortTrait() {
			return "sort:field";
		}
	}
}
