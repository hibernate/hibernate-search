/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DeleteLuceneWork extends LuceneWork {

	private static final long serialVersionUID = -854604138119230246L;

	public DeleteLuceneWork(Serializable id, String idInString, Class<?> entity) {
		this( null, id, idInString, entity );
	}

	public DeleteLuceneWork(String tenantId, Serializable id, String idInString, Class<?> entity) {
		super( tenantId, id, idInString, entity );
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitDeleteWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "DeleteLuceneWork" + tenant + ": " + this.getEntityClass().getName() + "#" + this.getIdInString();
	}

}
