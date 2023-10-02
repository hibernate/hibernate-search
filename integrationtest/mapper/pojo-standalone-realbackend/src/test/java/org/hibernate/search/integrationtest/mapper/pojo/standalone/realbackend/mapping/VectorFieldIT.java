/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VectorFieldIT {

	private static final String INDEX_NAME = "IndexName";
	private static final int BATCHES = 20;
	private static final int BATCH_SIZE = 1_000;

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withSingleBackend(
			MethodHandles.lookup(), BackendConfigurations.simple() );

	@BeforeAll
	static void beforeAll() {
		assumeTrue(
				isVectorSearchSupported(),
				"This test only makes sense if the backend supports vectors and vector search."
		);
	}

	/*
	 * While for the test of the max-allowed dimension it would be enough to index a single document and then search for it,
	 * in this case we want to generate more than a few documents to see how the backends,
	 * would handle the relatively high number of large vectors.
	 * For Lucene-specific limit tests see LuceneVectorFieldIT.
	 * As for the Elasticsearch/OpenSearch -- we only transmit the error-response from the backend to the user,
	 * so there's no need to do that much of extensive testing for this backend and `vectorSizeLimits_more_than_max` covers the basics.
	 */
	@Test
	void vectorSizeLimits_max_allowed_dimension_with_lots_of_documents() {
		// with OpenSearch 2.12 it allows up to 16000 which will lead to an OOM in this particular test:
		int maxDimension = Math.min( 4096, maxDimension() );
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			float[] floats;
			byte[] bytes;

			public IndexedEntity(Integer id) {
				this.id = id;
				this.floats = new float[maxDimension];
				this.bytes = new byte[maxDimension];
				Arrays.fill( floats, id / (float) maxDimension );
				Arrays.fill( floats, id * maxDimension % Byte.MAX_VALUE );
			}
		}

		SearchMapping setup = setupHelper.start().expectCustomBeans().withProperty(
				StandalonePojoMapperSettings.MAPPING_CONFIGURER,
				(StandalonePojoMappingConfigurer) context -> {
					TypeMappingStep book = context.programmaticMapping()
							.type( IndexedEntity.class );
					book.property( "floats" )
							.vectorField( maxDimension ).vectorSimilarity( VectorSimilarity.L2 );
					book.property( "bytes" )
							.vectorField( maxDimension ).vectorSimilarity( VectorSimilarity.L2 );
				}
		).setup( IndexedEntity.class );

		for ( int j = 0; j < BATCHES; j++ ) {
			try ( SearchSession session = setup.createSession() ) {
				SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
				for ( int i = 0; i < BATCH_SIZE; i++ ) {
					searchIndexingPlan.add( new IndexedEntity( i + j * BATCH_SIZE ) );
				}
			}
		}

		try ( SearchSession session = setup.createSession() ) {
			List<Object> bytes = session.search( IndexedEntity.class ).select( SearchProjectionFactory::id )
					.where( f -> f.knn( BATCHES ).field( "bytes" ).matching( new byte[maxDimension] ) )
					.fetchAllHits();
			assertThat( bytes ).hasSizeGreaterThanOrEqualTo( BATCHES );

			List<Object> floats = session.search( IndexedEntity.class ).select( SearchProjectionFactory::id )
					.where( f -> f.knn( BATCHES ).field( "floats" ).matching( new float[maxDimension] ) )
					.fetchAllHits();
			assertThat( floats ).hasSizeGreaterThanOrEqualTo( BATCHES );
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 10, 150, 500, 500000000 })
	void vectorSizeLimits_more_than_max(int increment) {
		int dimension = maxDimension() + increment;
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			float[] floats;
			byte[] bytes;

			public IndexedEntity(Integer id) {
				this.id = id;
				this.floats = new float[dimension];
				this.bytes = new byte[dimension];
				Arrays.fill( floats, id / (float) dimension );
				Arrays.fill( floats, id * dimension % Byte.MAX_VALUE );
			}
		}

		FailureReportChecker failure;
		if ( BackendConfiguration.isLucene() ) {
			String[] message = new String[] {
					"Vector 'dimension' cannot be equal to",
					Objects.toString( dimension ),
					"It must be a positive integer value lesser than or equal to"
			};
			failure = FailureReportUtils.hasFailureReport()
					.typeContext( IndexedEntity.class.getName() )
					.pathContext( ".floats" )
					.failure( message )
					.pathContext( ".bytes" )
					.failure( message );
		}
		else {
			failure = FailureReportUtils.hasFailureReport()
					.typeContext( IndexedEntity.class.getName() );
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().withProperty(
				StandalonePojoMapperSettings.MAPPING_CONFIGURER,
				(StandalonePojoMappingConfigurer) context -> {
					TypeMappingStep book = context.programmaticMapping()
							.type( IndexedEntity.class );
					book.property( "floats" )
							.vectorField( dimension ).vectorSimilarity( VectorSimilarity.L2 );
					book.property( "bytes" )
							.vectorField( dimension ).vectorSimilarity( VectorSimilarity.L2 );
				}
		).setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( failure );
	}

	/*
	* This test relies on a backend implementation to make sure that the vector dimension was somehow set for the field.
	* hence it requires a real backend.
	 */
	@Test
	void customBridge_vectorDimensionUnknown() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBinder = @ValueBinderRef(type = ValidImplicitTypeBridge.ValidImplicitTypeBinder.class))
			Collection<Float> floats;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".floats" )
						.indexContext( INDEX_NAME )
						.failure(
								"Invalid index field type: missing vector dimension."
										+ " Define the vector dimension explicitly."
										// hint:
										+ " Either specify dimension as an annotation property (@VectorField(dimension = somePositiveInteger)), or define a value binder (@VectorField(valueBinder = @ValueBinderRef(..))) that explicitly declares a vector field specifying the dimension."
						) );
	}

	private static int maxDimension() {
		if ( BackendConfiguration.isLucene() ) {
			return 4096;
		}
		else {
			ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
			ElasticsearchDistributionName distribution = actualVersion.distribution();
			if ( ElasticsearchDistributionName.ELASTIC.equals( distribution ) ) {
				return 4096;
			}
			else {
				// with OpenSearch 2.12 the max size for a lucene engine is also set to 16_000
				// and since serverless is using the latest OpenSearch build - we should treat it the same:
				if ( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS.equals( distribution )
						|| actualVersion.majorOptional().orElse( Integer.MIN_VALUE ) == 2
								&& ( actualVersion.minor().isEmpty() || actualVersion.minor().getAsInt() > 11 ) ) {
					return 16000;
				}
				else {
					return 1024;
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static class ValidImplicitTypeBridge implements ValueBridge<Collection, float[]> {

		public static class ValidImplicitTypeBinder implements ValueBinder {

			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( Collection.class, new ValidImplicitTypeBridge() );
			}
		}

		@Override
		public float[] toIndexedValue(Collection value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			float[] result = new float[value.size()];
			int index = 0;
			for ( Object o : value ) {
				result[index++] = Float.parseFloat( Objects.toString( o, null ) );
			}
			return result;
		}

		@Override
		public Collection fromIndexedValue(float[] value, ValueBridgeFromIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			List<Float> floats = new ArrayList<>( value.length );
			for ( float v : value ) {
				floats.add( v );
			}
			return floats;
		}
	}

	private static boolean isVectorSearchSupported() {
		return BackendConfiguration.isLucene()
				|| ElasticsearchTestDialect.isActualVersion(
						es -> !es.isLessThan( "8.12.0" ),
						os -> !os.isLessThan( "2.9.0" ),
						aoss -> true
				);
	}
}
