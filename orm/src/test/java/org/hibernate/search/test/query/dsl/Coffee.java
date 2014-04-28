/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.dsl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed
public class Coffee {

	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {
		this.id = id;
	}
	private Integer id;

	@Field(termVector = TermVector.NO, store = Store.YES)
	public String getName() { return name; }
	public void setName(String name) { this.name = name; };
	private String name;

	@Field(termVector = TermVector.YES)
	public String getSummary() { return summary; }
	public void setSummary(String summary) { this.summary = summary; }
	private String summary;

	@Column(length = 2000)
	@Field(termVector = TermVector.YES)
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;

	public int getIntensity() { return intensity; }
	public void setIntensity(int intensity) { this.intensity = intensity; }
	private int intensity;

	// Not stored nor term vector, i.e. cannot be used for More Like This
	@Field
	public String getInternalDescription() { return internalDescription; }
	public void setInternalDescription(String internalDescription) { this.internalDescription = internalDescription; }
	private String internalDescription;

	@ManyToOne
	@IndexedEmbedded(includeEmbeddedObjectId = true)
	public CoffeeBrand getBrand() { return brand; }
	public void setBrand(CoffeeBrand brand) { this.brand = brand; }
	private CoffeeBrand brand;

	@Override
	public String toString() {
		return "Coffee{" +
				"id=" + id +
				", name='" + name + '\'' +
				", summary='" + summary + '\'' +
				", description='" + description + '\'' +
				", intensity=" + intensity +
				'}';
	}
}
