/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;

import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;

import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.type.ComponentType;

/**
 * Order over multiple ID attributes.
 * <p>
 * This class should be used when target entity has multiple ID attributes,
 * e.g. an entity annotated {@link IdClass}, or an entity having an
 * {@link EmbeddedId}.
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
		this.propertyIndices = CollectionHelper.newArrayList( propertyPaths.size() );
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
	public Criterion idGreaterOrEqual(Object idObj) {
		return restrictLexicographically( Restrictions::ge, idObj );
	}

	@Override
	public Criterion idLesser(Object idObj) {
		return restrictLexicographically( Restrictions::lt, idObj );
	}

	@Override
	public void addAscOrder(Criteria criteria) {
		for ( String path : propertyPaths ) {
			criteria.addOrder( Order.asc( path ) );
		}
	}

	private Criterion restrictLexicographically(BiFunction<String, Object, SimpleExpression> lastRestriction, Object idObj) {
		Conjunction[] or = new Conjunction[propertyPaths.size()];

		for ( int i = 0; i < or.length; i++ ) {
			// Group expressions together in a single conjunction (A and B and C...).
			SimpleExpression[] and = new SimpleExpression[i + 1];
			int j = 0;
			for ( ; j < and.length - 1; j++ ) {
				// The first N-1 expressions have symbol `=`
				String path = propertyPaths.get( j );
				Object val = getPropertyValue( idObj, j );
				and[j] = Restrictions.eq( path, val );
			}
			// The last expression has whatever symbol is defined by "lastRestriction"
			String path = propertyPaths.get( j );
			Object val = getPropertyValue( idObj, j );
			and[j] = lastRestriction.apply( path, val );

			or[i] = Restrictions.conjunction( and );
		}
		// Group the disjunction of multiple expressions (X or Y or Z...).
		return Restrictions.or( or );
	}

	private Object getPropertyValue(Object obj, int ourIndex) {
		int theirIndex = propertyIndices.get( ourIndex );
		return componentType.getPropertyValue( obj, theirIndex );
	}
}
