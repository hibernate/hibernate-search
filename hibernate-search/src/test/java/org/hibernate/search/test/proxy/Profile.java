package org.hibernate.search.test.proxy;

import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Indexed
@Table(name = "profile")
@Proxy(proxyClass = IProfile.class)
public class Profile implements IProfile {

	private Integer id;
	private Set<IComment> comments;

	@Id
	@DocumentId
	@Column(name = "profileid")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(targetEntity = Comment.class, mappedBy = "profile")
	@IndexedEmbedded(targetElement = Comment.class)
    public Set<IComment> getComments() {
	    return comments;
    }
    public void setComments(Set<IComment> c) {
    	this.comments = c;
    }

}
