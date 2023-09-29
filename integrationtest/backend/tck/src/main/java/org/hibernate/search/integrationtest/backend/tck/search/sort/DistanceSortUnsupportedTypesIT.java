/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests basic behavior of distance sort common to all unsupported types,
 * i.e. error messages.
 */

class DistanceSortUnsupportedTypesIT<F> {

	private static Stream<FieldTypeDescriptor<?>> unsupportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> !GeoPoint.class.isAssignableFrom( typeDescriptor.getJavaType() ) );
	}

	public static List<? extends Arguments> params() {
		List<Arguments> parameters = new ArrayList<>();
		unsupportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			parameters.add( Arguments.of( fieldTypeDescriptor ) );
		} );
		return parameters;
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void error_notSupported(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = getFieldPath( fieldTypeDescriptor );

		assertThatThrownBy(
				() -> scope.sort().distance( absoluteFieldPath, GeoPoint.of( 42.0, 45.0 ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:distance' on field '" + absoluteFieldPath + "'",
						"'sort:distance' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				) );
	}

	private String getFieldPath(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		return index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedTypeDescriptors(), root, "", c -> {} );
		}
	}

}
