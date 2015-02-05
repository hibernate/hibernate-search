/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * @author Emmanuel Bernard
 */
public class DeleteLuceneWork extends LuceneWork {

	private static final long serialVersionUID = -854604138119230246L;

	public DeleteLuceneWork(Serializable id, String idInString, Class<?> entity) {
		super( id, idInString, entity );
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		return "DeleteLuceneWork: " + this.getEntityClass().getName() + "#" + this.getIdInString();
	}

}
