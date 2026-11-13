package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexRoot;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchSearchIndexScopeImpl;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonHandlerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchNativeArrayProjectionTest {
	@SuppressWarnings("unchecked")
	private final ProjectionExtractionHelper<String> typeNameHelper = mock( ProjectionExtractionHelper.class );

	@Test
	void separatelyIndexedObjectsProjectAsSeparateValuesForObjectAndGeneralHandlers() {
		ObjectNode first = object( "{\"value\":1,\"nested\":null}" );
		ObjectNode second = object( "{\"value\":2}" );
		assertObjectValues( JacksonJsonHandlers.object(), first, second );
		assertObjectValues( JacksonJsonHandlers.value(), first, second );
		assertObjectValues( GsonJsonHandlers.object(),
				GsonJsonHandlers.object().fromJson( first.toString() ),
				GsonJsonHandlers.object().fromJson( second.toString() ) );
		assertObjectValues( GsonJsonHandlers.value(),
				GsonJsonHandlers.value().fromJson( first.toString() ), GsonJsonHandlers.value().fromJson( second.toString() ) );
	}

	private <T> void assertObjectValues(JsonHandler<T> handler, T first, T second) {
		var index = new TestIndex<>( handler.javaType(), typedCodec( handler ), true, "multi" );
		var document = index.document();
		document.addValue( index.reference, first );
		assertThat( project( index, document.build() ) ).containsExactly( first );
		document.addValue( index.reference, second );
		assertThat( document.build() ).isEqualTo( object( "{\"field\":[{\"value\":1,\"nested\":null},{\"value\":2}]}" ) );
		assertThat( project( index, document.build() ) ).containsExactly( first, second );
	}

	@Test
	void typedArraysRemainAtomicForSingleValuedFieldsAndInsideMultiValuedFields() {
		JsonNode first = JacksonJsonHandlers.value().fromJson( "[1,null,{\"nested\":null}]" );
		var single = new TestIndex<>( JsonNode.class, typedCodec( JacksonJsonHandlers.value() ), false, "single" );
		var singleDocument = single.document();
		singleDocument.addValue( single.reference, first );
		assertThat( singleDocument.build().get( "field" ) ).isSameAs( first );
		assertThat( project( single, singleDocument.build() ) ).containsExactly( first );

		var multi = new TestIndex<>( JsonNode.class, typedCodec( JacksonJsonHandlers.value() ), true, "multi" );
		var multiDocument = multi.document();
		multiDocument.addValue( multi.reference, first );
		assertThat( multiDocument.build() ).isEqualTo( object( "{\"field\":[[1,null,{\"nested\":null}]]}" ) );
		assertThat( project( multi, multiDocument.build() ) ).containsExactly( first );
		// Reusing a borrowed array must not append to it or introduce a self-cycle.
		multiDocument.addValue( multi.reference, first );
		multiDocument.addValue( multi.reference, JacksonJsonHandlers.value().fromJson( "[]" ) );
		JsonTreeValidation.validateExternal( multiDocument.build() );
		assertThat( first ).hasSize( 3 );
		assertThat( project( multi, multiDocument.build() ) ).containsExactly( first, first,
				JacksonJsonHandlers.value().fromJson( "[]" ) );
	}

	@Test
	void missingNullAndEmptyArrayKeepDistinctCardinality() {
		var multi = new TestIndex<>( ObjectNode.class, typedCodec( JacksonJsonHandlers.object() ), true, "multi" );
		var document = multi.document();
		assertThat( project( multi, document.build() ) ).isEmpty();
		document.addValue( multi.reference, null );
		assertThat( project( multi, document.build() ) ).containsExactly( (ObjectNode) null );
		ObjectNode value = object( "{\"value\":1}" );
		document.addValue( multi.reference, value );
		document.addValue( multi.reference, null );
		assertThat( project( multi, document.build() ) ).containsExactly( null, value, null );
		// Accept an unwrapped scalar/null returned by documents indexed outside Search as one value.
		assertThat( project( multi, object( "{\"field\":null}" ) ) ).containsExactly( (ObjectNode) null );
		assertThat( project( multi, object( "{\"field\":{\"value\":1}}" ) ) ).containsExactly( value );
		assertThat( project( multi, object( "{\"field\":[]}" ) ) ).isEmpty();

		var single = new TestIndex<>( JsonNode.class, typedCodec( JacksonJsonHandlers.value() ), false, "single" );
		assertThat( project( single, object( "{}" ) ) ).isEmpty();
		assertThat( project( single, object( "{\"field\":null}" ) ) ).containsExactly( (JsonNode) null );
		assertThat( project( single, object( "{\"field\":[]}" ) ) )
				.containsExactly( JacksonJsonHandlers.value().fromJson( "[]" ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	void multiIndexProjectionUsesEachHitsFieldCardinality() {
		var codec = typedCodec( JacksonJsonHandlers.value() );
		var single = new TestIndex<>( JsonNode.class, codec, false, "single" );
		var multi = new TestIndex<>( JsonNode.class, codec, true, "multi" );
		var singleDocument = single.document();
		JsonNode atomicArray = JacksonJsonHandlers.value().fromJson( "[1,null,2]" );
		singleDocument.addValue( single.reference, atomicArray );
		var multiDocument = multi.document();
		ObjectNode first = object( "{\"value\":1}" );
		ObjectNode second = object( "{\"value\":2}" );
		multiDocument.addValue( multi.reference, first );
		multiDocument.addValue( multi.reference, second );
		var scope = scope( single, multi );
		var field = (ElasticsearchSearchIndexValueFieldContext<JsonNode>) scope.field( "field" ).toValueField();
		var projection = new ElasticsearchFieldProjection.Factory<>( codec )
				.create( scope, field )
				.type( JsonNode.class, ValueModel.INDEX ).build( ProjectionCollector.list() );
		var request = object( "{}" );
		var context = requestContext();
		var extractor = ( (ElasticsearchSearchProjection<List<JsonNode>>) projection ).request( request, context );
		verify( typeNameHelper ).request( request, context );
		ObjectNode singleHit = object( "{\"_index\":\"single-000001\"}" );
		ObjectNode multiHit = object( "{\"_index\":\"multi-000001\"}" );
		when( typeNameHelper.extract( singleHit, null ) ).thenReturn( "single" );
		when( typeNameHelper.extract( multiHit, null ) ).thenReturn( "multi" );
		assertThat( finish( extractor, singleHit, singleDocument.build() ) ).containsExactly( atomicArray );
		assertThat( finish( extractor, multiHit, multiDocument.build() ) ).containsExactly( first, second );
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedNativeArraysRetainSplittingAndHistoricalIndexingShape() {
		var codec = new ElasticsearchJsonHandlerFieldCodec<>( JsonProviderHelper.create( false ).getCodec(),
				GsonJsonHandlers.value() );
		var single = new TestIndex<>( JsonElement.class, codec, false, "single" );
		var value = GsonJsonHandlers.value().fromJson( "[1,null,{\"nested\":null}]" );
		var document = single.document();
		document.addValue( single.reference, value );
		assertThat( document.build() ).isEqualTo( object( "{\"field\":[1,null,{\"nested\":null}]}" ) );
		assertThat( project( single, document.build() ) ).containsExactly(
				GsonJsonHandlers.value().fromJson( "1" ), null, GsonJsonHandlers.value().fromJson( "{\"nested\":null}" ) );
		var multi = new TestIndex<>( JsonElement.class, codec, true, "multi" );
		var multiDocument = multi.document();
		multiDocument.addValue( multi.reference, GsonJsonHandlers.value().fromJson( "1" ) );
		assertThat( multiDocument.build() ).isEqualTo( object( "{\"field\":1}" ) );
		multiDocument.addValue( multi.reference, GsonJsonHandlers.value().fromJson( "2" ) );
		assertThat( multiDocument.build() ).isEqualTo( object( "{\"field\":[1,2]}" ) );
		assertThat( project( multi, multiDocument.build() ) ).containsExactly(
				GsonJsonHandlers.value().fromJson( "1" ), GsonJsonHandlers.value().fromJson( "2" ) );
	}

	@Test
	void defaultGsonAndJacksonArraysKeepHistoricalIndexingAndProjectionCardinality() {
		assertSplittingDefault( GsonJsonHandlers.value() );
		assertSplittingDefault( JacksonJsonHandlers.value() );
	}

	private <T> void assertSplittingDefault(JsonHandler<T> handler) {
		var codec = new ElasticsearchJsonHandlerFieldCodec<>( JsonProviderHelper.create( false ).getCodec(), handler );
		for ( boolean multiValued : new boolean[] { false, true } ) {
			var index = new TestIndex<>( handler.javaType(), codec, multiValued, "default" );
			var document = index.document();
			assertThat( project( index, document.build() ) ).isEmpty();
			document.addValue( index.reference, handler.fromJson( "[1,null,{\"nested\":null}]" ) );
			assertThat( document.build() ).isEqualTo( object( "{\"field\":[1,null,{\"nested\":null}]}" ) );
			assertThat( project( index, document.build() ) ).containsExactly( handler.fromJson( "1" ), null,
					handler.fromJson( "{\"nested\":null}" ) );
			var empty = index.document();
			empty.addValue( index.reference, handler.fromJson( "[]" ) );
			assertThat( empty.build() ).isEqualTo( object( "{\"field\":[]}" ) );
			assertThat( project( index, empty.build() ) ).isEmpty();
			var scalar = index.document();
			scalar.addValue( index.reference, handler.fromJson( "1" ) );
			assertThat( scalar.build() ).isEqualTo( object( "{\"field\":1}" ) );
			assertThat( project( index, scalar.build() ) ).containsExactly( handler.fromJson( "1" ) );
			var nil = index.document();
			nil.addValue( index.reference, null );
			assertThat( project( index, nil.build() ) ).containsExactly( (T) null );
			if ( multiValued ) {
				scalar.addValue( index.reference, handler.fromJson( "2" ) );
				assertThat( scalar.build() ).isEqualTo( object( "{\"field\":[1,2]}" ) );
				assertThat( project( index, scalar.build() ) ).containsExactly( handler.fromJson( "1" ),
						handler.fromJson( "2" ) );
			}
		}
	}

	@Test
	void gsonAtomicArraysPreserveEmptyAndSeparatelyIndexedArrays() {
		var handler = GsonJsonHandlers.value();
		var index = new TestIndex<>( handler.javaType(), typedCodec( handler ), true, "atomic" );
		var document = index.document();
		var first = handler.fromJson( "[1,null]" );
		var empty = handler.fromJson( "[]" );
		document.addValue( index.reference, first );
		document.addValue( index.reference, empty );
		document.addValue( index.reference, first );
		assertThat( document.build() ).isEqualTo( object( "{\"field\":[[1,null],[],[1,null]]}" ) );
		assertThat( project( index, document.build() ) ).containsExactly( first, empty, first );
	}

	@Test
	void splitModeOwnsAccumulationArrayWithoutMutatingReusedJacksonInputs() {
		var handler = JacksonJsonHandlers.value();
		var codec = new ElasticsearchJsonHandlerFieldCodec<>( JsonProviderHelper.create( false ).getCodec(), handler );
		var index = new TestIndex<>( JsonNode.class, codec, true, "default" );
		var document = index.document();
		JsonNode array = handler.fromJson( "[1,2]" );
		document.addValue( index.reference, array );
		document.addValue( index.reference, array );
		document.addValue( index.reference, handler.fromJson( "3" ) );
		assertThat( array ).isEqualTo( handler.fromJson( "[1,2]" ) );
		assertThat( document.build() ).isEqualTo( object( "{\"field\":[1,2,[1,2],3]}" ) );
		JsonTreeValidation.validateExternal( document.build() );
		assertThat( project( index, document.build() ) ).containsExactly( handler.fromJson( "1" ), handler.fromJson( "2" ),
				array,
				handler.fromJson( "3" ) );
	}

	@Test
	void arrayOnlyHandlerRoundTripsInExplicitAtomicMode() {
		JsonHandler<tools.jackson.databind.node.ArrayNode> handler = new JsonHandler<>() {
			public Class<tools.jackson.databind.node.ArrayNode> javaType() {
				return tools.jackson.databind.node.ArrayNode.class;
			}

			public String toJson(tools.jackson.databind.node.ArrayNode value) {
				return JacksonJsonHandlers.value().toJson( value );
			}

			public tools.jackson.databind.node.ArrayNode fromJson(String text) {
				return javaType().cast( JacksonJsonHandlers.value().fromJson( text ) );
			}
		};
		var index = new TestIndex<>( handler.javaType(), typedCodec( handler ), true, "array-only" );
		var document = index.document();
		var empty = handler.fromJson( "[]" );
		var value = handler.fromJson( "[1,null]" );
		document.addValue( index.reference, empty );
		document.addValue( index.reference, value );
		assertThat( project( index, document.build() ) ).containsExactly( empty, value );
	}

	@Test
	void multiIndexProjectionRejectsDifferentArrayModes() {
		var json = JsonProviderHelper.create( false ).getCodec();
		var handler = JacksonJsonHandlers.value();
		var defaultCodec = new ElasticsearchJsonHandlerFieldCodec<>( json, handler );
		var atomicCodec = new ElasticsearchJsonHandlerFieldCodec<>( json, handler, true );
		var split = new TestIndex<>( JsonNode.class, defaultCodec, true, "split" );
		var atomic = new TestIndex<>( JsonNode.class, atomicCodec, true, "atomic" );
		var scope = scope( split, atomic );
		assertThatThrownBy( () -> scope.fieldQueryElement( "field",
				org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys.FIELD ) )
				.isInstanceOf( org.hibernate.search.util.common.SearchException.class ).hasMessageContaining( "codec" );
	}

	private <T> List<T> project(TestIndex<T> index, ObjectNode source) {
		var projection = new ElasticsearchFieldProjection.Factory<>( index.field.type().codec() )
				.create( scope( index ), index.field ).type( index.field.type().valueClass(), ValueModel.INDEX )
				.build( ProjectionCollector.list() );
		var extractor = ( (ElasticsearchSearchProjection<List<T>>) projection ).request( object( "{}" ), requestContext() );
		return finish( extractor, null, source );
	}

	private <T, A> List<T> finish(ElasticsearchSearchProjection.Extractor<A, List<T>> extractor,
			ObjectNode hit, ObjectNode source) {
		A extracted = extractor.extract( null, hit, source, null );
		return extractor.transform( null, extracted, mock( ProjectionTransformContext.class ) );
	}

	private ProjectionRequestContext requestContext() {
		var context = mock( ProjectionRequestContext.class );
		when( context.forField( "field", new String[] { "field" } ) ).thenReturn( context );
		when( context.relativeCurrentFieldPathComponents() ).thenReturn( new String[] { "field" } );
		when( context.projectionCardinalityCorrectlyAddressed( null ) ).thenReturn( true );
		return context;
	}

	private ElasticsearchSearchIndexScopeImpl<Object> scope(TestIndex<?>... indexes) {
		var backend = mock( SearchBackendContext.class );
		when( backend.getSearchProjectionBackendContext() )
				.thenReturn( new SearchProjectionBackendContext( typeNameHelper, null ) );
		return new ElasticsearchSearchIndexScopeImpl<>( mock( BackendMappingContext.class ), Object.class, backend,
				JsonProviderHelper.create( false ).getCodec(), null, null, null,
				Set.of( Arrays.stream( indexes ).map( i -> i.model ).toArray( ElasticsearchIndexModel[]::new ) ) );
	}

	private static <T> ElasticsearchJsonHandlerFieldCodec<T> typedCodec(JsonHandler<T> handler) {
		return new ElasticsearchJsonHandlerFieldCodec<>( JsonProviderHelper.create( false ).getCodec(), handler, true );
	}

	private static ObjectNode object(String text) {
		return JacksonJsonHandlers.object().fromJson( text );
	}

	private static class TestIndex<T> {
		private final ElasticsearchIndexModel model = mock( ElasticsearchIndexModel.class );
		private final ElasticsearchIndexValueField<T> field;
		private final ElasticsearchIndexFieldReference<T> reference = new ElasticsearchIndexFieldReference<>();

		TestIndex(Class<T> javaType, ElasticsearchFieldCodec<T> codec, boolean multiValued, String typeName) {
			var children = new LinkedHashMap<String, ElasticsearchIndexField>();
			var root = new ElasticsearchIndexRoot( new ElasticsearchIndexCompositeNodeType.Builder( ObjectStructure.DEFAULT )
					.build(), children );
			var typeBuilder = new ElasticsearchIndexValueFieldType.Builder<>( javaType, new PropertyMapping() );
			typeBuilder.codec( codec );
			typeBuilder.queryElementFactory( org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys.FIELD,
					new ElasticsearchFieldProjection.Factory<>( codec ) );
			field = new ElasticsearchIndexValueField<>( root, "field", typeBuilder.build(), TreeNodeInclusion.INCLUDED,
					multiValued );
			children.put( "field", field );
			reference.setSchemaNode( field );
			when( model.root() ).thenReturn( root );
			when( model.fieldOrNull( "field" ) ).thenReturn( field );
			when( model.hibernateSearchName() ).thenReturn( typeName );
			when( model.mappedTypeName() ).thenReturn( typeName );
		}

		ElasticsearchDocumentObjectBuilder document() {
			return new ElasticsearchDocumentObjectBuilder( model );
		}
	}
}
