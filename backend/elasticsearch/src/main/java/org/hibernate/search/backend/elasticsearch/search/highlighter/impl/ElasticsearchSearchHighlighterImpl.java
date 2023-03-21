/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.highlighter.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchSearchHighlighterImpl implements ElasticsearchSearchHighlighter {

	public static final ElasticsearchSearchHighlighter NO_OPTIONS_CONFIGURATION = new ElasticsearchSearchHighlighterImpl(
			Collections.emptySet(), null, null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null
	);

	private static final JsonObjectAccessor REQUEST_HIGHLIGHT_ACCESSOR = JsonAccessor.root().property( "highlight" ).asObject();
	private static final JsonAccessor<String> TYPE = JsonAccessor.root().property( "type" ).asString();
	private static final JsonAccessor<String> BOUNDARY_CHARS = JsonAccessor.root().property( "boundary_chars" ).asString();
	private static final JsonAccessor<Integer> BOUNDARY_MAX_SCAN = JsonAccessor.root().property( "boundary_max_scan" ).asInteger();
	private static final JsonAccessor<String> ENCODER = JsonAccessor.root().property( "encoder" ).asString();
	private static final JsonAccessor<Integer> FRAGMENT_SIZE = JsonAccessor.root().property( "fragment_size" ).asInteger();
	private static final JsonAccessor<Integer> NO_MATCH_SIZE = JsonAccessor.root().property( "no_match_size" ).asInteger();
	private static final JsonAccessor<Integer> NUMBER_OF_FRAGMENTS = JsonAccessor.root().property( "number_of_fragments" ).asInteger();
	private static final JsonAccessor<String> ORDER = JsonAccessor.root().property( "order" ).asString();
	private static final JsonAccessor<Integer> MAX_ANALYZED_OFFSET = JsonAccessor.root().property( "max_analyzed_offset" ).asInteger();
	private static final JsonAccessor<String> TAGS_SCHEMA = JsonAccessor.root().property( "tags_schema" ).asString();
	private static final JsonArrayAccessor PRE_TAGS = JsonAccessor.root().property( "pre_tags" ).asArray();
	private static final JsonArrayAccessor POST_TAGS = JsonAccessor.root().property( "post_tags" ).asArray();
	private static final JsonAccessor<String> BOUNDARY_SCANNER = JsonAccessor.root().property( "boundary_scanner" ).asString();
	private static final JsonAccessor<String> BOUNDARY_SCANNER_LOCALE = JsonAccessor.root().property( "boundary_scanner_locale" ).asString();
	private static final JsonAccessor<String> FRAGMENTER = JsonAccessor.root().property( "fragmenter" ).asString();
	private static final JsonAccessor<Integer> PHRASE_LIMIT = JsonAccessor.root().property( "phrase_limit" ).asInteger();

	private final Set<String> indexNames;
	private final SearchHighlighterType type;
	private final String boundaryChars;
	private final Integer boundaryMaxScan;
	private final Integer fragmentSize;
	private final Integer noMatchSize;
	private final Integer numberOfFragments;
	private final String orderByScore;
	private final Integer maxAnalyzedOffset;
	private final List<String> preTags;
	private final List<String> postTags;
	private final String boundaryScannerType;
	private final String boundaryScannerLocale;
	private final String fragmenterType;
	private final Integer phraseLimit;
	private final String encoder;
	private final String tagSchema;

	private ElasticsearchSearchHighlighterImpl(Builder builder) {
		this(
				builder.scope.hibernateSearchIndexNames(),
				builder.type(),
				builder.boundaryCharsAsString(),
				builder.boundaryMaxScan(), builder.fragmentSize(), builder.noMatchSize(), builder.numberOfFragments(),
				Boolean.TRUE.equals( builder.orderByScore() ) ? "score" : null, builder.maxAnalyzedOffset(), builder.preTags(),
				builder.postTags(),
				convertBoundaryScannerType( builder.boundaryScannerType() ),
				Objects.toString( builder.boundaryScannerLocale(), null ),
				convertHighlighterFragmenter( builder.fragmenterType() ),
				builder.phraseLimit(), convertHighlighterEncoder( builder.encoder() ),
				convertHighlighterTagSchema( builder.tagSchema() )
		);
	}

	private ElasticsearchSearchHighlighterImpl(Set<String> indexNames, SearchHighlighterType type, String boundaryChars,
			Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize, Integer numberOfFragments,
			String orderByScore, Integer maxAnalyzedOffset, List<String> preTags, List<String> postTags,
			String boundaryScannerType, String boundaryScannerLocale, String fragmenterType,
			Integer phraseLimit, String encoder, String tagSchema) {
		this.indexNames = indexNames;
		this.type = type;
		this.boundaryChars = boundaryChars;
		this.boundaryMaxScan = boundaryMaxScan;
		this.fragmentSize = fragmentSize;
		this.noMatchSize = noMatchSize;
		this.numberOfFragments = numberOfFragments;
		this.orderByScore = orderByScore;
		this.maxAnalyzedOffset = maxAnalyzedOffset;
		this.preTags = preTags;
		this.postTags = postTags;
		this.boundaryScannerType = boundaryScannerType;
		this.boundaryScannerLocale = boundaryScannerLocale;
		this.fragmenterType = fragmenterType;
		this.phraseLimit = phraseLimit;
		this.encoder = encoder;
		this.tagSchema = tagSchema;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + indexNames
				+ "]";
	}

	@Override
	public void request(JsonObject requestBody) {
		Optional<JsonObject> highlightOptional = REQUEST_HIGHLIGHT_ACCESSOR.get( requestBody );
		if ( highlightOptional.isPresent() ) {
			toJson( highlightOptional.get() );
		}
		else {
			log.noFieldsToHighlight();
		}
	}

	@Override
	public void applyToField(String path, JsonObject fields) {
		fields.add( path, toJson( new JsonObject() ) );
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public SearchHighlighterType type() {
		return type;
	}

	private JsonObject toJson(JsonObject result) {
		setIfNotNull( TYPE, convertHighlighterType( this.type ), result );
		setIfNotNull( BOUNDARY_CHARS, this.boundaryChars, result );
		setIfNotNull( BOUNDARY_MAX_SCAN, this.boundaryMaxScan, result );
		setIfNotNull( FRAGMENT_SIZE, this.fragmentSize, result );
		setIfNotNull( NO_MATCH_SIZE, this.noMatchSize, result );
		setIfNotNull( NUMBER_OF_FRAGMENTS, this.numberOfFragments, result );
		setIfNotNull( ORDER, this.orderByScore, result );
		setIfNotNull( MAX_ANALYZED_OFFSET, this.maxAnalyzedOffset, result );
		setIfNotNull( BOUNDARY_SCANNER, this.boundaryScannerType, result );
		setIfNotNull( BOUNDARY_SCANNER_LOCALE, this.boundaryScannerLocale, result );
		setIfNotNull( FRAGMENTER, this.fragmenterType, result );
		setIfNotNull( PHRASE_LIMIT, this.phraseLimit, result );
		setIfNotEmpty( PRE_TAGS, this.preTags, result );
		setIfNotEmpty( POST_TAGS, this.postTags, result );
		setIfNotNull( ENCODER, this.encoder, result );
		setIfNotNull( TAGS_SCHEMA, this.tagSchema, result );

		return result;
	}

	public static class Builder extends SearchHighlighterBuilder {

		private final ElasticsearchSearchIndexScope<?> scope;

		public Builder(ElasticsearchSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		public ElasticsearchSearchHighlighter build() {
			return new ElasticsearchSearchHighlighterImpl( this );
		}
	}

	private static <T> void setIfNotNull(JsonAccessor<T> accessor, T value, JsonObject object) {
		if ( value != null ) {
			accessor.set( object, value );
		}
	}

	private static void setIfNotEmpty(JsonArrayAccessor accessor, Collection<String> values, JsonObject object) {
		if ( values != null && !values.isEmpty() ) {
			JsonArray array = new JsonArray();
			values.forEach( array::add );
			accessor.set( object, array );
		}
	}
	private static String convertHighlighterType(SearchHighlighterType type) {
		if ( type == null ) {
			return null;
		}
		switch ( type ) {
			case UNIFIED:
				return "unified";
			case PLAIN:
				return "plain";
			case FAST_VECTOR:
				return "fvh";
			default:
				throw new IllegalStateException( "Unknown highlighter type: " + type );
		}
	}

	private static String convertBoundaryScannerType(BoundaryScannerType type) {
		switch ( type ) {
			case DEFAULT :
				return null;
			case CHARS:
				return "chars";
			case SENTENCE:
				return "sentence";
			case WORD:
				return "word";
			default:
				throw new IllegalStateException( "Unknown boundary scanner type: " + type );
		}
	}

	private static String convertHighlighterFragmenter(HighlighterFragmenter fragmenter) {
		if ( fragmenter == null ) {
			return null;
		}
		switch ( fragmenter ) {
			case SIMPLE:
				return "simple";
			case SPAN:
				return "span";
			default:
				throw new IllegalStateException( "Unknown fragmenter: " + fragmenter );
		}
	}

	private static String convertHighlighterEncoder(HighlighterEncoder encoder) {
		if ( encoder == null ) {
			return null;
		}
		switch ( encoder ) {
			case DEFAULT:
				return "default";
			case HTML:
				return "html";
			default:
				throw new IllegalStateException( "Unknown encoder: " + encoder );
		}
	}

	private static String convertHighlighterTagSchema(HighlighterTagSchema tagSchema) {
		if ( tagSchema == null ) {
			return null;
		}
		if ( HighlighterTagSchema.STYLED.equals( tagSchema ) ) {
			return "styled";
		}
		throw new IllegalStateException( "Unknown tag schema: " + tagSchema );
	}
}
