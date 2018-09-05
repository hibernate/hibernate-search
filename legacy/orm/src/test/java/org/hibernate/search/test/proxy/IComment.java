/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.proxy;

import java.util.List;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
public interface IComment {

	Integer getId();

	void setId(Integer id);

	IProfile getProfile();

	void setProfile(IProfile p);

	String getContent();

	void setContent(String name);

	IComment getRootComment();

	void setRootComment(IComment root);

	List<IComment> getReplies();

	void setReplies(List<IComment> replies);

}
