/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * A place where documents are available.
 */
@Entity
@Indexed
@GeoPointBinding(fieldName = "location", sortable = Sortable.YES)
public class Library extends AbstractEntity<Integer> {

	@Id
	private Integer id;

	@Basic
	// TODO HSEARCH-3465 use multi-fields here
	@FullTextField(projectable = Projectable.YES)
	@KeywordField(
			name = "name_sort",
			normalizer = LibraryAnalyzers.NORMALIZER_SORT,
			sortable = Sortable.YES
	)
	private String name;

	@Basic
	@GenericField(sortable = Sortable.YES, aggregable = Aggregable.YES)
	private Integer collectionSize;

	@Basic
	@Latitude
	private Double latitude;

	@Basic
	@Longitude
	private Double longitude;

	@ElementCollection
	@GenericField(aggregable = Aggregable.YES, projectable = Projectable.YES)
	private List<LibraryServiceOption> services;

	@OneToMany(mappedBy = "library", cascade = CascadeType.REMOVE)
	private List<DocumentCopy<?>> copies = new ArrayList<>();

	public Library() {
	}

	public Library(int id, String name, int collectionSize, double latitude, double longitude,
			LibraryServiceOption... services) {
		this.id = id;
		this.name = name;
		this.collectionSize = collectionSize;
		this.latitude = latitude;
		this.longitude = longitude;
		this.services = Arrays.asList( services );
	}

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

	public List<LibraryServiceOption> getServices() {
		return services;
	}

	public void setServices(List<LibraryServiceOption> services) {
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
