/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Order over a single ID attribute.
 * <p>
 * This class should be used when target entity has a single ID attribute.
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public class SingularIdOrder implements IdOrder {

	private final String idAttributeName;

	public SingularIdOrder(SingularAttribute<?, ?> idAttribute) {
		super();
		this.idAttributeName = idAttribute.getName();
	}

	@Override
	public Criterion idGreaterOrEqual(Object idObj) {
		return Restrictions.ge( idAttributeName, idObj );
	}

	@Override
	public Criterion idLesser(Object idObj) {
		return Restrictions.lt( idAttributeName, idObj );
	}

	@Override
	public void addAscOrder(Criteria criteria) {
		criteria.addOrder( Order.asc( idAttributeName ) );
	}

}
