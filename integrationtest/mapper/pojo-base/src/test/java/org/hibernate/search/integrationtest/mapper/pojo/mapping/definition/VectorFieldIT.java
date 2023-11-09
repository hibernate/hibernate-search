/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class VectorFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void beamWidth() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, beamWidth = 10)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ).beamWidth( 10 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void maxConnections() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, maxConnections = 10)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ).maxConnections( 10 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, name = "explicitName")
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", byte[].class, f -> f.dimension( 5 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void projectable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, projectable = Projectable.YES)
			byte[] projectableYes;
			@VectorField(dimension = 4, projectable = Projectable.NO)
			byte[] projectableNo;
			@VectorField(dimension = 4, projectable = Projectable.DEFAULT)
			byte[] projectableDefault;
			@VectorField(dimension = 4)
			byte[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "projectableYes", byte[].class, f -> f.dimension( 4 ).projectable( Projectable.YES ) )
				.field( "projectableNo", byte[].class, f -> f.dimension( 4 ).projectable( Projectable.NO ) )
				.field( "projectableDefault", byte[].class, f -> f.dimension( 4 ) )
				.field( "implicit", byte[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void searchable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, searchable = Searchable.YES)
			byte[] searchableYes;
			@VectorField(dimension = 4, searchable = Searchable.NO)
			byte[] searchableNo;
			@VectorField(dimension = 4, searchable = Searchable.DEFAULT)
			byte[] searchableDefault;
			@VectorField(dimension = 4)
			byte[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchableYes", byte[].class, f -> f.dimension( 4 ).searchable( Searchable.YES ) )
				.field( "searchableNo", byte[].class, f -> f.dimension( 4 ).searchable( Searchable.NO ) )
				.field( "searchableDefault", byte[].class, f -> f.dimension( 4 ) )
				.field( "implicit", byte[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void vectorSimilarity() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.L2)
			float[] vectorSimilarityL2;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.COSINE)
			float[] vectorSimilarityCosine;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.INNER_PRODUCT)
			float[] vectorSimilarityInnerProduct;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.DEFAULT)
			float[] vectorSimilarityDefault;
			@VectorField(dimension = 4)
			float[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vectorSimilarityL2", float[].class, f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.L2 ) )
				.field( "vectorSimilarityCosine", float[].class,
						f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.COSINE ) )
				.field( "vectorSimilarityInnerProduct", float[].class,
						f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.INNER_PRODUCT ) )
				.field( "vectorSimilarityDefault", float[].class, f -> f.dimension( 4 ) )
				.field( "implicit", float[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}


}
