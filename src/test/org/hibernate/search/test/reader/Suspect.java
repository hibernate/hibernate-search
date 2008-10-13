//$Id$
package org.hibernate.search.test.reader;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Suspect {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;
	@Field(index = Index.TOKENIZED)
	private String name;
	@Field(index = Index.TOKENIZED)
	private String physicalDescription;
	@Field(index = Index.TOKENIZED)
	@Column(length = 500)
	private String suspectCharge;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhysicalDescription() {
		return physicalDescription;
	}

	public void setPhysicalDescription(String physicalDescription) {
		this.physicalDescription = physicalDescription;
	}

	public String getSuspectCharge() {
		return suspectCharge;
	}

	public void setSuspectCharge(String suspectCharge) {
		this.suspectCharge = suspectCharge;
	}
}
