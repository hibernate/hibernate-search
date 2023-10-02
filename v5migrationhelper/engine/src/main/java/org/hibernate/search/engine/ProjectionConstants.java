/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * Defined projection constants.
 * <p>
 * Implementator's note: When adding new constants, be sure to add them to
 * {@code LuceneHSQuery#SUPPORTED_PROJECTION_CONSTANTS}, too.
 *
 * @author Emmanuel Bernard
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface ProjectionConstants {
	/**
	 * Represents the Hibernate entity returned in a search.
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link SearchProjectionFactory#entity()}.
	 */
	@Deprecated
	String THIS = "__HSearch_This";

	/**
	 * The Lucene document returned by a search.
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link LuceneSearchProjectionFactory#document()}.
	 * You will need to pass {@link LuceneExtension#get()} to
	 * {@link SearchProjectionFactory#extension(SearchProjectionFactoryExtension)}
	 * in order to get access to this Lucene-specific feature.
	 */
	@Deprecated
	String DOCUMENT = "__HSearch_Document";

	/**
	 * The legacy document's score from a search.
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link SearchProjectionFactory#score()}.
	 */
	@Deprecated
	String SCORE = "__HSearch_Score";

	/**
	 * Object id property
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link SearchProjectionFactory#entityReference()};
	 * call {@code .id()} on the reference to get the entity identifier.
	 */
	@Deprecated
	String ID = "__HSearch_id";

	/**
	 * Lucene {@link org.apache.lucene.search.Explanation} object describing the score computation for
	 * the matching object/document
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination)
	 * To retrieve explanation of a single result, consider retrieving {@link #ID the entity id}
	 * and using fullTextQuery.explain(Object)
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link LuceneSearchProjectionFactory#explanation()}.
	 * You will need to pass {@link LuceneExtension#get()} to
	 * {@link SearchProjectionFactory#extension(SearchProjectionFactoryExtension)}
	 * in order to get access to this Lucene-specific feature.
	 */
	@Deprecated
	String EXPLANATION = "__HSearch_Explanation";

	/**
	 * Represents the Hibernate entity class returned in a search. In contrast to the other constants this constant
	 * represents an actual field value of the underlying Lucene document and hence can directly be used in queries.
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link SearchProjectionFactory#entityReference()};
	 * call {@code .type()} on the reference to get the entity type.
	 */
	@Deprecated
	String OBJECT_CLASS = "_hibernate_class";

	/**
	 * Represents the distance (in kilometers) between an entity and the
	 * center of the search area in case of a spatial query.
	 * @deprecated See the javadoc of this class for how to create projections in Hibernate Search 6.
	 * The equivalent projection in Hibernate Search 6 is {@link SearchProjectionFactory#distance(String, GeoPoint)}.
	 */
	@Deprecated
	String SPATIAL_DISTANCE = "_HSearch_SpatialDistance";
}
