/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * A place where documents are available.
 */
@Entity
@Indexed(index = Library.INDEX)
@GeoPointBridge(fieldName = "location")
public class Library extends AbstractEntity<Integer> {

	static final String INDEX = "Library";

	@Id
	private Integer id;

	@Basic
	// TODO use multi-fields here
	// TODO use a different analyzer/normalizer for these fields
	@Field(analyzer = "default")
	@Field(name = "name_sort", sortable = Sortable.YES)
	private String name;

	@Basic
	@Field(sortable = Sortable.YES)
	private Integer collectionSize;

	@Basic
	@Latitude
	private Double latitude;

	@Basic
	@Longitude
	private Double longitude;

	@ElementCollection
	@Field
	private List<LibraryService> services;

	@OneToMany(mappedBy = "library")
	private List<DocumentCopy<?>> copies = new ArrayList<>();

	@Override
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

	public int getCollectionSize() {
		return collectionSize;
	}

	public void setCollectionSize(int collectionSize) {
		this.collectionSize = collectionSize;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public List<LibraryService> getServices() {
		return services;
	}

	public void setServices(List<LibraryService> services) {
		this.services = services;
	}

	public List<DocumentCopy<?>> getCopies() {
		return copies;
	}

	@Override
	protected String getDescriptionForToString() {
		return getName();
	}
}
