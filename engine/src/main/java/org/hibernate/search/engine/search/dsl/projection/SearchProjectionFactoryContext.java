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
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.function.TriFunction;

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
	 * Project to a field of the indexed document.
	 *
	 * Unlike {@link #field(String, Class)} this method bypass any {@code DslConverter} defined on the field,
	 * so that the values will be provided exactly as they are stored in the backend.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param type The resulting type of the projection.
	 * @param <T> The resulting type of the projection.
	 * @return A context allowing to define the projection more precisely.
	 */
	<T> FieldProjectionContext<T> rawField(String absoluteFieldPath, Class<T> type);

	/**
	 * Project to a field of the indexed document without specifying a type.
	 *
	 * Unlike {@link #field(String)} this method bypass any {@code DslConverter} defined on the field,
	 * so that the values will be provided exactly as they are stored in the backend.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @return A context allowing to define the projection more precisely.
	 */
	FieldProjectionContext<Object> rawField(String absoluteFieldPath);

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
	 * Create a projection that will compose a {@link List} based on the given almost-built projections.
	 *
	 * @param terminalContexts The terminal contexts allowing to retrieve {@link SearchProjection}s.
	 * @return A context allowing to define the projection more precisely.
	 */
	default CompositeProjectionContext<List<?>> composite(SearchProjectionTerminalContext<?> ... terminalContexts) {
		return composite( Function.identity(), terminalContexts );
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
	 * Create a projection that will compose a custom object based on the given almost-built projections.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param terminalContexts The terminal contexts allowing to retrieve {@link SearchProjection}s.
	 * @return A context allowing to define the projection more precisely.
	 */
	default <T> CompositeProjectionContext<T> composite(Function<List<?>, T> transformer,
			SearchProjectionTerminalContext<?> ... terminalContexts) {
		SearchProjection<?>[] projections = new SearchProjection<?>[terminalContexts.length];
		for ( int i = 0; i < terminalContexts.length; i++ ) {
			projections[i] = terminalContexts[i].toProjection();
		}
		return composite( transformer, projections );
	}

	/**
	 * Create a projection that will compose a custom object based on one given projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param projection The original projection used to produce the element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	<P, T> CompositeProjectionContext<T> composite(Function<P, T> transformer, SearchProjection<P> projection);

	/**
	 * Create a projection that will compose a custom object based on one almost-built projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param terminalContext The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	default <P, T> CompositeProjectionContext<T> composite(Function<P, T> transformer, SearchProjectionTerminalContext<P> terminalContext) {
		return composite( transformer, terminalContext.toProjection() );
	}

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
	 * Create a projection that will compose a custom object based on two almost-built projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param terminalContext1 The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the first element passed to the transformer.
	 * @param terminalContext2 The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the second element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	default <P1, P2, T> CompositeProjectionContext<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjectionTerminalContext<P1> terminalContext1, SearchProjectionTerminalContext<P2> terminalContext2) {
		return composite(
				transformer,
				terminalContext1.toProjection(), terminalContext2.toProjection()
		);
	}

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

	/**
	 * Create a projection that will compose a custom object based on three almost-built projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param terminalContext1 The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the first element passed to the transformer.
	 * @param terminalContext2 The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the second element passed to the transformer.
	 * @param terminalContext3 The terminal context allowing to retrieve the {@link SearchProjection}
	 * that will be used to produce the third element passed to the transformer.
	 * @return A context allowing to define the projection more precisely.
	 */
	default <P1, P2, P3, T> CompositeProjectionContext<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjectionTerminalContext<P1> terminalContext1, SearchProjectionTerminalContext<P2> terminalContext2,
			SearchProjectionTerminalContext<P3> terminalContext3) {
		return composite(
				transformer,
				terminalContext1.toProjection(), terminalContext2.toProjection(), terminalContext3.toProjection()
		);
	}

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering different types of projections.
	 *
	 * @param extension The extension to the projection DSL.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchProjectionFactoryContextExtension<T, R, O> extension);

	/**
	 * Create a context allowing to try to apply multiple extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchProjectionFactoryContextExtension)} method instead.
	 * <p>
	 * This method is generic, and you should set the generic type explicitly to the expected projected type,
	 * e.g. {@code .<MyProjectedType>extension()}.
	 *
	 * @param <T> The expected projected type.
	 * @return A context allowing to define the extensions to attempt, and the corresponding projections.
	 */
	<T> SearchProjectionFactoryExtensionContext<T, R, O> extension();
}
