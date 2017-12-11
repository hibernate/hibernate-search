/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.Indexable;
import org.hibernate.search.mapper.pojo.model.spi.IndexableReference;


/**
 * @author Yoann Rodiere
 */
public class PojoIndexable implements Indexable {

	private final Object root;

	public PojoIndexable(Object root) {
		super();
		this.root = root;
	}

	@Override
	public <T> T get(IndexableReference<T> key) {
		return ((PojoIndexableReference<T>) key).get( root );
	}

}
