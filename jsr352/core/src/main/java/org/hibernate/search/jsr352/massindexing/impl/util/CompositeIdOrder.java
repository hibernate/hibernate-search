/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.search.util.impl.CollectionHelper;

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

	private final String prefix;

	private final List<String> idAttributeNames;

	public CompositeIdOrder(String prefix, Collection<? extends SingularAttribute<?, ?>> idAttributes) {
		super();
		this.prefix = prefix;
		this.idAttributeNames = CollectionHelper.newArrayList( idAttributes.size() );
		for ( SingularAttribute<?, ?> idAttribute : idAttributes ) {
			String name = idAttribute.getName();
			idAttributeNames.add( name );
		}
		idAttributeNames.sort( Comparator.naturalOrder() );

	}

	@Override
	public Criterion idGreaterOrEqual(Object idObj) throws Exception {
		return restrictLexicographically( Restrictions::ge, idObj );
	}

	@Override
	public Criterion idLesser(Object idObj) throws Exception {
		return restrictLexicographically( Restrictions::lt, idObj );
	}

	@Override
	public void addAscOrder(Criteria criteria) {
		for ( String name : idAttributeNames ) {
			criteria.addOrder( Order.asc( prefix + name ) );
		}
	}

	private Criterion restrictLexicographically(BiFunction<String, Object, SimpleExpression> lastRestriction, Object idObj)
			throws InvocationTargetException, IllegalAccessException, IntrospectionException {
		Conjunction[] or = new Conjunction[idAttributeNames.size()];

		for ( int i = 0; i < or.length; i++ ) {
			// Group expressions together in a single conjunction (A and B and C...).
			SimpleExpression[] and = new SimpleExpression[i + 1];
			int j = 0;
			for ( ; j < and.length - 1; j++ ) {
				// The first N-1 expressions have symbol `=`
				String key = idAttributeNames.get( j );
				Object val = getPropertyValue( idObj, key );
				and[j] = Restrictions.eq( prefix + key, val );
			}
			// The last expression has whatever symbol is defined by "lastRestriction"
			String key = idAttributeNames.get( j );
			Object val = getPropertyValue( idObj, key );
			and[j] = lastRestriction.apply( prefix + key, val );

			or[i] = Restrictions.conjunction( and );
		}
		// Group the disjunction of multiple expressions (X or Y or Z...).
		return Restrictions.or( or );
	}

	private static Object getPropertyValue(Object obj, String propertyName)
			throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		return new PropertyDescriptor( propertyName, obj.getClass() ).getReadMethod().invoke( obj );
	}
}
