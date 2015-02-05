/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * A unit of work used to purge an entire index.
 *
 * @author John Griffin
 */
public class PurgeAllLuceneWork extends LuceneWork {

	private static final long serialVersionUID = 8124091288284011715L;

	public PurgeAllLuceneWork(Class<?> entity) {
		super( null, null, entity, null );
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		return "PurgeAllLuceneWork: " + this.getEntityClass().getName();
	}

}
