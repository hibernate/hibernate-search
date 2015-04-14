/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;


/**
 * A unit of work used to purge an entire index.
 *
 * @author John Griffin
 */
public class PurgeAllLuceneWork extends LuceneWork {

	private static final long serialVersionUID = 8124091288284011715L;

	public PurgeAllLuceneWork(Class<?> entity) {
		this( null, entity );
	}

	public PurgeAllLuceneWork(String tenantId, Class<?> entity) {
		super( tenantId, null, null, entity, null );
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitPurgeAllWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "PurgeAllLuceneWork" + tenant + ": " + this.getEntityClass().getName();
	}

}
