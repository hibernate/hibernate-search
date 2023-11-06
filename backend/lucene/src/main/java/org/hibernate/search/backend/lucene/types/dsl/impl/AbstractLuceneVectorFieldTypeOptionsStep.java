/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import static org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat.DEFAULT_MAX_DIMENSIONS;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneVectorFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.backend.lucene.types.dsl.LuceneVectorFieldTypeOptionsStep;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
abstract class AbstractLuceneVectorFieldTypeOptionsStep<S extends AbstractLuceneVectorFieldTypeOptionsStep<?, F>, F>
		extends AbstractLuceneIndexFieldTypeOptionsStep<S, F>
		implements LuceneVectorFieldTypeOptionsStep<S, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final int MAX_BEAM_WIDTH = 3200;
	private static final int MAX_MAX_CONNECTIONS = 512;

	protected VectorSimilarity vectorSimilarity = VectorSimilarity.DEFAULT;
	protected int dimension = -1;
	protected int beamWidth = MAX_MAX_CONNECTIONS;
	protected int maxConnections = 16;
	private Projectable projectable = Projectable.DEFAULT;
	private Searchable searchable = Searchable.DEFAULT;
	private F indexNullAsValue = null;

	AbstractLuceneVectorFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> valueType) {
		super( buildContext, valueType );
	}

	@Override
	public S dimension(int dimension) {
		if ( dimension < 1 || dimension > DEFAULT_MAX_DIMENSIONS ) {
			throw log.vectorPropertyUnsupportedValue( "dimension", dimension, DEFAULT_MAX_DIMENSIONS );
		}
		this.dimension = dimension;
		return thisAsS();
	}

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		this.searchable = searchable;
		return thisAsS();
	}

	@Override
	public S vectorSimilarity(VectorSimilarity vectorSimilarity) {
		this.vectorSimilarity = vectorSimilarity;
		return thisAsS();
	}

	@Override
	public S beamWidth(int beamWidth) {
		if ( beamWidth < 1 || beamWidth > MAX_BEAM_WIDTH ) {
			throw log.vectorPropertyUnsupportedValue( "beamWidth", beamWidth, MAX_BEAM_WIDTH );
		}
		this.beamWidth = beamWidth;
		return thisAsS();
	}

	@Override
	public S maxConnections(int maxConnections) {
		if ( maxConnections < 1 || maxConnections > MAX_MAX_CONNECTIONS ) {
			throw log.vectorPropertyUnsupportedValue( "maxConnections", maxConnections, MAX_MAX_CONNECTIONS );
		}
		this.maxConnections = maxConnections;
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAsValue) {
		this.indexNullAsValue = indexNullAsValue;
		return thisAsS();
	}

	@Override
	public LuceneIndexValueFieldType<F> toIndexFieldType() {
		if ( dimension < 0 ) {
			// means we never called dimension(..)
			throw log.vectorDimensionNotSpecified( buildContext.getEventContext() );
		}

		VectorSimilarity resolvedVectorSimilarity = resolveDefault( vectorSimilarity );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		Indexing indexing = resolvedSearchable ? Indexing.ENABLED : Indexing.DISABLED;
		Storage storage = resolvedProjectable ? Storage.ENABLED : Storage.DISABLED;

		AbstractLuceneVectorFieldCodec<F, ?> codec = createCodec( resolvedVectorSimilarity, dimension, storage, indexing,
				indexNullAsValue, new HibernateSearchKnnVectorsFormat( maxConnections, beamWidth )
		);
		builder.codec( codec );
		if ( resolvedSearchable ) {
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new LuceneExistsPredicate.DocValuesOrNormsBasedFactory<>() );
			builder.searchable( true );
		}

		return builder.build();
	}

	protected abstract AbstractLuceneVectorFieldCodec<F, ?> createCodec(VectorSimilarity vectorSimilarity, int dimension,
			Storage storage, Indexing indexing, F indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat);

	protected static VectorSimilarity resolveDefault(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DEFAULT:
			case L2:
				return VectorSimilarity.L2;
			case INNER_PRODUCT:
				return VectorSimilarity.INNER_PRODUCT;
			case COSINE:
				return VectorSimilarity.COSINE;
			default:
				throw new AssertionFailure( "Unexpected value for Similarity: " + vectorSimilarity );
		}
	}

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
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

}
