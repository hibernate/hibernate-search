/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard
 */
@Indexed
class Coffee {

	public static final String NAME_SORT = "name_sort";

	@DocumentId
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private String id;

	@Field(termVector = TermVector.NO, store = Store.YES)
	@Field(name = NAME_SORT, analyze = Analyze.NO)
	@SortableField(forField = NAME_SORT)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@Field(termVector = TermVector.YES)
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	private String summary;

	@Field(termVector = TermVector.YES)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String description;

	@Field
	public int getIntensity() {
		return intensity;
	}

	public void setIntensity(int intensity) {
		this.intensity = intensity;
	}

	private int intensity;

	// Not stored nor term vector, i.e. cannot be used for More Like This
	@Field
	public String getInternalDescription() {
		return internalDescription;
	}

	public void setInternalDescription(String internalDescription) {
		this.internalDescription = internalDescription;
	}

	private String internalDescription;

	@IndexedEmbedded(includeEmbeddedObjectId = true)
	public CoffeeBrand getBrand() {
		return brand;
	}

	public void setBrand(CoffeeBrand brand) {
		this.brand = brand;
	}

	private CoffeeBrand brand;

	@IndexedEmbedded(includeEmbeddedObjectId = true)
	public CoffeeMaker getMaker() {
		return maker;
	}

	public void setMaker(CoffeeMaker maker) {
		this.maker = maker;
	}

	private CoffeeMaker maker;

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
