/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.function.TriFunction;

/**
 * A context allowing to create a projection.
 *
 * @param <R> The type of references, i.e. the type of objects returned for {@link #reference() reference projections}.
 * @param <O> The type of loaded objects, i.e. the type of objects returned for
 * {@link #object() object projections}.
 */
public interface SearchProjectionFactoryContext<R, O> {

	/**
	 * Project the match to a {@link DocumentReference}.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	DocumentReferenceProjectionContext documentReference();

	/**
	 * Project to a reference to the match.
	 * <p>
	 * The actual type of the reference depends on the mapper used to create the query:
	 * a POJO mapper may return a class/identifier couple, for example.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	ReferenceProjectionContext<R> reference();

	/**
	 * Project to an object representing the match.
	 * <p>
	 * The actual type of the object depends on the entry point
	 * for your query: an {@link org.hibernate.search.engine.backend.index.IndexManager}
	 * will return a Java representation of the document,
	 * but a mapper may return a Java representation of the mapped object.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	ObjectProjectionContext<O> object();

	/**
	 * Project to a field of the indexed document.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param type The resulting type of the projection.
	 * @param <T> The resulting type of the projection.
	 * @return A context allowing to define the projection more precisely.
	 */
	<T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> type);

	/**
	 * Project to a field of the indexed document without specifying a type.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @return A context allowing to define the projection more precisely.
	 */
	FieldProjectionContext<Object> field(String absoluteFieldPath);

	/**
	 * Project on the score of the hit.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	ScoreProjectionContext score();

	/**
	 * Project on the distance from the center to a {@link GeoPoint} field.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	DistanceToFieldProjectionContext distance(String absoluteFieldPath, GeoPoint center);

	/**
	 * Create a projection that will compose a {@link List} based on the given projections.
	 *
	 * @param projections The projections used to populate the list, in order.
	 * @return A context allowing to define the projection more precisely.
	 */
	default CompositeProjectionContext<List<?>> composite(SearchProjection<?>... projections) {
		return composite( Function.identity(), projections );
	}

	/**
	 * Create a projection that will compose a custom object based on the given projections.
	 *
	 * @param transformer The function that will transform the list of projected elements into a custom object.
	 * @param projections The projections used to populate the list, in order.
	 * @return A context allowing to define the projection more precisely.
	 */
	<T> CompositeProjectionContext<T> composite(Function<List<?>, T> transformer, SearchProjection<?>... projections);

	/**
	 * Create a projection that will compose a custom object based on one given projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param projection The original projection used to produce the element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	<P, T> CompositeProjectionContext<T> composite(Function<P, T> transformer, SearchProjection<P> projection);

	/**
	 * Create a projection that will compose a custom object based on two given projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param projection1 The projection used to produce the first element passed to the transformer.
	 * @param projection2 The projection used to produce the second element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	<P1, P2, T> CompositeProjectionContext<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2);

	/**
	 * Create a projection that will compose a custom object based on three given projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param projection1 The projection used to produce the first element passed to the transformer.
	 * @param projection2 The projection used to produce the second element passed to the transformer.
	 * @param projection3 The projection used to produce the third element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	<P1, P2, P3, T> CompositeProjectionContext<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3);
}
