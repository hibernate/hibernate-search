/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldHighlightProjection;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchPredicateTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTermsPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextMatchPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextPhrasePredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextRegexpPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextWildcardPredicate;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSort;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonPrimitive;


class ElasticsearchStringIndexFieldTypeOptionsStep
		extends AbstractElasticsearchStandardIndexFieldTypeOptionsStep<ElasticsearchStringIndexFieldTypeOptionsStep, String>
		implements StringIndexFieldTypeOptionsStep<ElasticsearchStringIndexFieldTypeOptionsStep> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private String searchAnalyzerName;
	private String normalizerName;
	private Projectable projectable = Projectable.DEFAULT;
	private Searchable searchable = Searchable.DEFAULT;
	private Norms norms = Norms.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;
	private Aggregable aggregable = Aggregable.DEFAULT;
	private String indexNullAs;
	private TermVector termVector = TermVector.DEFAULT;

	ElasticsearchStringIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		this.searchAnalyzerName = searchAnalyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep projectable(Projectable projectable) {
		this.projectable = projectable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep norms(Norms norms) {
		this.norms = norms;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep termVector(TermVector termVector) {
		this.termVector = termVector;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep indexNullAs(String indexNullAs) {
		this.indexNullAs = indexNullAs;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep searchable(Searchable searchable) {
		this.searchable = searchable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep aggregable(Aggregable aggregable) {
		this.aggregable = aggregable;
		return this;
	}

	@Override
	public IndexFieldType<String> toIndexFieldType() {
		PropertyMapping mapping = builder.mapping();

		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		mapping.setIndex( resolvedSearchable );

		if ( analyzerName != null ) {
			mapping.setType( DataTypes.TEXT );
			mapping.setAnalyzer( analyzerName );
			mapping.setSearchAnalyzer( searchAnalyzerName );
			mapping.setTermVector( resolveTermVector() );

			builder.analyzerName( analyzerName );
			builder.searchAnalyzerName( searchAnalyzerName );
			builder.queryElementFactory( ProjectionTypeKeys.HIGHLIGHT, new ElasticsearchFieldHighlightProjection.Factory<>() );

			if ( normalizerName != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, buildContext.getEventContext() );
			}

			if ( resolvedSortable ) {
				throw log.cannotUseAnalyzerOnSortableField( analyzerName, buildContext.getEventContext() );
			}

			if ( indexNullAs != null ) {
				throw log.cannotUseIndexNullAsAndAnalyzer( analyzerName, indexNullAs, buildContext.getEventContext() );
			}

			if ( resolvedAggregable ) {
				throw log.cannotUseAnalyzerOnAggregableField( analyzerName, buildContext.getEventContext() );
			}
		}
		else {
			mapping.setType( DataTypes.KEYWORD );
			mapping.setNormalizer( normalizerName );
			mapping.setDocValues( resolvedSortable || resolvedAggregable );

			builder.normalizerName( normalizerName );

			if ( searchAnalyzerName != null ) {
				throw log.searchAnalyzerWithoutAnalyzer( searchAnalyzerName, buildContext.getEventContext() );
			}
		}

		mapping.setNorms( resolveNorms() );

		if ( indexNullAs != null ) {
			mapping.setNullValue( new JsonPrimitive( indexNullAs ) );
		}

		ElasticsearchStringFieldCodec codec = ElasticsearchStringFieldCodec.INSTANCE;
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH, new ElasticsearchTextMatchPredicate.Factory( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new ElasticsearchRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.PHRASE, new ElasticsearchTextPhrasePredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.WILDCARD, new ElasticsearchTextWildcardPredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.REGEXP, new ElasticsearchTextRegexpPredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new ElasticsearchTermsPredicate.Factory<>( codec ) );
			builder.queryElementFactory( ElasticsearchPredicateTypeKeys.SIMPLE_QUERY_STRING,
					new ElasticsearchSimpleQueryStringPredicateBuilderFieldState.Factory() );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD, new ElasticsearchStandardFieldSort.TextFieldFactory( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
		}

		return builder.build();
	}

	@Override
	protected ElasticsearchStringIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private boolean resolveNorms() {
		switch ( norms ) {
			case YES:
				return true;
			case NO:
				return false;
			case DEFAULT:
				return ( analyzerName != null );
			default:
				throw new AssertionFailure( "Unexpected value for Norms: " + norms );
		}
	}

	private String resolveTermVector() {
		switch ( termVector ) {
			case NO:
			case DEFAULT:
				return "no";
			default:
				return termVector.name().toLowerCase( Locale.ROOT );
		}
	}
}
