package org.hibernate.search.test.proxy;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
@Entity
@Table(name = "comment")
@Proxy(proxyClass = IComment.class)
public class Comment implements IComment {

	private Integer id;
	private IProfile parent;
	private String name;
	private IComment root;
	private List<IComment> replies = new ArrayList<IComment>();

	@Id
	@Column(name = "commentid")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = Profile.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "profileid")
	@ContainedIn
    public IProfile getProfile() {
		if (parent == null && getRootComment() != null) {
			return getRootComment().getProfile();
		}
	    return parent;
    }
    public void setProfile(IProfile p) {
    	this.parent = p;
    }

	@Column(name = "content")
	@Field(name = "content")
	public String getContent() {
		return name;
	}
	public void setContent(String name) {
		this.name = name;
	}

	@ManyToOne(targetEntity = Comment.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "rootid")
	public IComment getRootComment() {
		return root;
	}
	public void setRootComment(IComment root) {
		this.root = root;
	}

	@OneToMany(targetEntity = Comment.class, mappedBy = "rootComment", fetch = FetchType.LAZY)
	@Cascade(CascadeType.DELETE)
	public List<IComment> getReplies() {
		return replies;
	}

	public void setReplies(List<IComment> replies) {
		this.replies = replies;
	}
}
