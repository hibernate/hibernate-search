/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.AbstractElasticsearchVectorFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractElasticsearchVectorFieldTypeOptionsStep<
		S extends AbstractElasticsearchVectorFieldTypeOptionsStep<?, F>,
		F> extends AbstractElasticsearchIndexFieldTypeOptionsStep<S, F>
		implements VectorFieldTypeOptionsStep<S, F>, ElasticsearchVectorFieldTypeMappingContributor.Context {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchVectorFieldTypeMappingContributor mappingContributor;

	protected VectorSimilarity vectorSimilarity = VectorSimilarity.DEFAULT;
	protected Integer dimension;
	protected Integer efConstruction;
	protected Integer m;
	protected F indexNullAs;
	private Projectable projectable = Projectable.DEFAULT;
	private Searchable searchable = Searchable.DEFAULT;

	AbstractElasticsearchVectorFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, ElasticsearchVectorFieldTypeMappingContributor mappingContributor) {
		super( buildContext, fieldType, new PropertyMapping() );
		this.mappingContributor = mappingContributor;
	}

	@Override
	public S searchable(Searchable searchable) {
		this.searchable = searchable;
		return thisAsS();
	}

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		this.indexNullAs = indexNullAs;
		return thisAsS();
	}

	@Override
	public S vectorSimilarity(VectorSimilarity vectorSimilarity) {
		this.vectorSimilarity = vectorSimilarity;
		return thisAsS();
	}

	@Override
	public S efConstruction(int efConstruction) {
		this.efConstruction = efConstruction;
		return thisAsS();
	}

	@Override
	public S m(int m) {
		this.m = m;
		return thisAsS();
	}


	@Override
	public S dimension(int dimension) {
		this.dimension = dimension;
		return thisAsS();
	}

	@Override
	public ElasticsearchIndexValueFieldType<F> toIndexFieldType() {
		if ( dimension == null ) {
			throw log.nullVectorDimension( buildContext.hints().missingVectorDimension(), buildContext.getEventContext() );
		}
		PropertyMapping mapping = builder.mapping();

		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		mapping.setIndex( resolvedSearchable );
		mappingContributor.contribute( mapping, this );

		AbstractElasticsearchVectorFieldCodec<F> codec =
				createCodec( vectorSimilarity, dimension, m, efConstruction, indexNullAs );
		builder.codec( codec );
		if ( resolvedSearchable ) {
			builder.searchable( true );
			mappingContributor.contribute( builder, this );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>() );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
		}

		builder.multivaluable( false );

		return builder.build();
	}

	protected abstract AbstractElasticsearchVectorFieldCodec<F> createCodec(VectorSimilarity vectorSimilarity, int dimension,
			Integer m, Integer efConstruction, F indexNullAs);

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case YES:
				return true;
			case NO:
				return false;
			default:
				throw new AssertionFailure( "Unexpected value for Projectable: " + projectable );
		}
	}

	protected static boolean resolveDefault(Searchable searchable) {
		switch ( searchable ) {
			case DEFAULT:
			case YES:
				return true;
			case NO:
				return false;
			default:
				throw new AssertionFailure( "Unexpected value for Searchable: " + searchable );
		}
	}

	@Override
	public abstract String type();

	@Override
	public boolean searchable() {
		return resolveDefault( searchable );
	}

	@Override
	public VectorSimilarity vectorSimilarity() {
		return vectorSimilarity;
	}

	@Override
	public int dimension() {
		return dimension;
	}

	@Override
	public Integer efConstruction() {
		return efConstruction;
	}

	@Override
	public Integer m() {
		return m;
	}
}
