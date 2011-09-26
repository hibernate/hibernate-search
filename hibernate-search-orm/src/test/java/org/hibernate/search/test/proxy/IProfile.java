package org.hibernate.search.test.proxy;

import java.util.Set;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
public interface IProfile {

	public Integer getId();

	public void setId(Integer id);

	public Set<IComment> getComments();

	public void setComments(Set<IComment> c);
}
