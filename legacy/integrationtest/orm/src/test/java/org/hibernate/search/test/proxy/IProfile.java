/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.proxy;

import java.util.Set;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
public interface IProfile {

	Integer getId();

	void setId(Integer id);

	Set<IComment> getComments();

	void setComments(Set<IComment> c);
}
