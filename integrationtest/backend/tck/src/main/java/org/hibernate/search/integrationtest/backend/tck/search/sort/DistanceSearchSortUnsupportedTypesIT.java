/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of distance sort common to all unsupported types,
 * i.e. error messages.
 */
@RunWith(Parameterized.class)
public class DistanceSearchSortUnsupportedTypesIT<F> {

	private static Stream<FieldTypeDescriptor<?>> unsupportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> !GeoPoint.class.isAssignableFrom( typeDescriptor.getJavaType() ) );
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		unsupportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
				parameters.add( new Object[] { fieldTypeDescriptor } );
		} );
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static IndexMapping indexMapping;
	private static StubMappingIndexManager indexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> DistanceSearchSortUnsupportedTypesIT.indexManager = indexManager
				)
				.setup();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public DistanceSearchSortUnsupportedTypesIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	public void error_notSupported() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = getFieldPath();

		SubTest.expectException(
				() -> scope.sort().distance( absoluteFieldPath, GeoPoint.of( 42.0, 45.0 ) )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Distance related operations are not supported", absoluteFieldPath );
	}

	private String getFieldPath() {
		return indexMapping.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static class IndexMapping {
		final SimpleFieldModelsByType fieldModels;

		IndexMapping(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedTypeDescriptors(), root, "", c -> { } );
		}
	}

}
