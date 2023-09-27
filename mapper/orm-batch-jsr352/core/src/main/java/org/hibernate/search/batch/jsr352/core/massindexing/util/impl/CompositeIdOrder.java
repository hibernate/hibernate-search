/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;

import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.type.ComponentType;

/**
 * Order over multiple ID attributes.
 * <p>
 * This class should be used when target entity has multiple ID attributes,
 * e.g. an entity annotated {@link IdClass}, or an entity having an
 * {@link EmbeddedId}.
 * <p>
 * By default, {@code Restrictions.ge} and similar on
 * a composite property will generate a non-lexicographical "greater or equal" condition.
 * <p>
 * For instance, a condition such as
 * {@code Restrictions.ge("myEmbeddedDate", MyEmbeddedDate.yearMonthDay( 2017, 6, 20 )}
 * will generate {@code year >= 2017 AND month >= 6 AND day >= 20}
 * which is obviously not what any sane person would expect,
 * since this will exclude July 1st to July 19th (among others).
 * <p>
 * This class fixes the issue by properly implementing lexicographical
 * "greater or equal" and "lesser" conditions.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-2615">HSEARCH-2615</a>
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public class CompositeIdOrder implements IdOrder {

	private final ComponentType componentType;

	private final List<String> propertyPaths;

	private final List<Integer> propertyIndices;

	public CompositeIdOrder(String componentPath, ComponentType componentType) {
		super();
		this.componentType = componentType;

		// Initialize with relative paths, but prepend a prefix below
		this.propertyPaths = new ArrayList<>( Arrays.asList( componentType.getPropertyNames() ) );
		this.propertyPaths.sort( Comparator.naturalOrder() );

		String pathPrefix = componentPath == null ? "" : componentPath + ".";
		this.propertyIndices = new ArrayList<>( propertyPaths.size() );
		ListIterator<String> iterator = this.propertyPaths.listIterator();
		while ( iterator.hasNext() ) {
			String propertyName = iterator.next();

			// We need the relative path of the property here
			propertyIndices.add( componentType.getPropertyIndex( propertyName ) );

			// Prepend the path prefix to each property; we will only use absolute path from now on
			iterator.set( pathPrefix + propertyName );
		}
	}

	@Override
	public Predicate idGreater(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return restrictLexicographically( builder::greaterThan, builder, root, idObj, false );
	}

	@Override
	public Predicate idGreaterOrEqual(CriteriaBuilder builder, Root<?> root, Object idObj) {
		// Caution, using 'builder::greaterThanOrEqualTo' here won't cut it, we really need
		// to separate the strict operator from the equals.
		return restrictLexicographically( builder::greaterThan, builder, root, idObj, true );
	}

	@Override
	public Predicate idLesser(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return restrictLexicographically( builder::lessThan, builder, root, idObj, false );
	}

	@Override
	public void addAscOrder(CriteriaBuilder builder, CriteriaQuery<?> criteria, Root<?> root) {
		ArrayList<Order> orders = new ArrayList<>( propertyPaths.size() );
		for ( String pathString : propertyPaths ) {
			orders.add( builder.asc( toPath( root, pathString ) ) );
		}
		criteria.orderBy( orders );
	}

	@SuppressWarnings("unchecked")
	private Predicate restrictLexicographically(
			BiFunction<Expression<Comparable<? super Object>>, Comparable<? super Object>, Predicate> strictOperator,
			CriteriaBuilder builder, Root<?> root, Object idObj, boolean orEquals) {
		int propertyPathsSize = propertyPaths.size();
		int expressionsInOr = propertyPathsSize + ( orEquals ? 1 : 0 );

		Predicate[] or = new Predicate[expressionsInOr];

		for ( int i = 0; i < propertyPathsSize; i++ ) {
			// Group expressions together in a single conjunction (A and B and C...).
			Predicate[] and = new Predicate[i + 1];
			int j = 0;
			for ( ; j < and.length - 1; j++ ) {
				// The first N-1 expressions have symbol `=`
				and[j] = builder.equal( toPath( root, propertyPaths.get( j ) ),
						getPropertyValue( idObj, j ) );
			}
			// The last expression has whatever symbol is defined by "strictOperator"
			and[j] = strictOperator.apply( toPath( root, propertyPaths.get( j ) ),
					(Comparable<? super Object>) getPropertyValue( idObj, j ) );

			or[i] = builder.and( and );
		}

		if ( orEquals ) {
			Predicate[] and = new Predicate[propertyPathsSize];
			for ( int i = 0; i < propertyPathsSize; i++ ) {
				and[i] = builder.equal( toPath( root, propertyPaths.get( i ) ),
						getPropertyValue( idObj, i ) );
			}
			or[or.length - 1] = builder.and( and );
		}

		// Group the disjunction of multiple expressions (X or Y or Z...).
		return builder.or( or );
	}

	@SuppressWarnings("unchecked")
	private <T> Path<T> toPath(Path<?> parent, String pathString) {
		Path<?> result = parent;
		for ( String pathElement : pathString.split( "\\." ) ) {
			result = result.get( pathElement );
		}
		return (Path<T>) result;
	}

	private Object getPropertyValue(Object obj, int ourIndex) {
		int theirIndex = propertyIndices.get( ourIndex );
		return componentType.getPropertyValue( obj, theirIndex );
	}
}
