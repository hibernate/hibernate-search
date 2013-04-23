package org.hibernate.search.test.proxy;

import java.util.List;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
public interface IComment {

	public Integer getId();

	public void setId(Integer id);

	public IProfile getProfile();

	public void setProfile(IProfile p);

	public String getContent();

	public void setContent(String name);

	public IComment getRootComment();

	public void setRootComment(IComment root);

	public List<IComment> getReplies();

	public void setReplies(List<IComment> replies);

}
