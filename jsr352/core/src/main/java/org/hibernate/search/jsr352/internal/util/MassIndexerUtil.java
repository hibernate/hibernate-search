/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.util;

import org.hibernate.Session;
import org.hibernate.search.hcore.util.impl.ContextHelper;

/**
 * @author Mincong Huang
 */
public class MassIndexerUtil {

	public static String getIdName(Class<?> clazz, Session session) {
		return ContextHelper.getSearchintegrator( session )
				.getIndexBindings()
				.get( clazz )
				.getDocumentBuilder()
				.getIdentifierName();
	}
}
