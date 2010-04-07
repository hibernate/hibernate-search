package org.hibernate.search.test.embedded.nested.containedIn;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Indexed
@Entity
public class HelpItem {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version()
	private Long version;

	@Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.NO)
	private String title;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "helpItem")
	@IndexedEmbedded
	private List<HelpItemTag> tags;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<HelpItemTag> getTags() {
		if ( tags == null ) {
			tags = new ArrayList<HelpItemTag>();
		}
		return tags;
	}

	public void setTags(List<HelpItemTag> tags) {
		this.tags = tags;
	}
}