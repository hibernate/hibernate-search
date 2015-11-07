/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.genericjpa.annotations.IdColumn;
import org.hibernate.search.genericjpa.annotations.IdInfo;
import org.hibernate.search.genericjpa.annotations.InIndex;
import org.hibernate.search.genericjpa.annotations.UpdateInfo;
import org.hibernate.search.genericjpa.db.ColumnType;

@Entity
@Table(name = "SORCERER")
@InIndex
@Indexed
@UpdateInfo(tableName = "SORCERER", idInfos = {
		@IdInfo(columns = @IdColumn(column = "ID", columnType = ColumnType.INTEGER))
})
public class Sorcerer {

	//automatic generation is a bitch if you want to have it accross multiple database types
	//and/or persistence providers
	private static final AtomicInteger HACK = new AtomicInteger();

	private Integer id = HACK.incrementAndGet();
	private String name;
	private Place place;

	@Id
	@Column(name = "ID")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(store = Store.NO, index = Index.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Sorcerer [id=" + this.getId() + ", name=" + this.getName() + "]";
	}

	@ContainedIn
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinTable(name = "Place_Sorcerer")
	@UpdateInfo(tableName = "Place_Sorcerer", updateTableName = "PlaceSorcererUpdatesHsearch", updateTableIdColumn = "updateid", updateTableEventTypeColumn = "eventCase", idInfos = {
			@IdInfo(entity = Place.class, columns = @IdColumn(column = "Place_ID", updateTableColumn = "placefk", columnType = ColumnType.INTEGER)),
			@IdInfo(entity = Sorcerer.class, columns = @IdColumn(column = "Sorcerer_ID", updateTableColumn = "sorcererfk", columnType = ColumnType.INTEGER))
	})
	@IndexedEmbedded(includeEmbeddedObjectId = true)
	public Place getPlace() {
		return place;
	}

	public void setPlace(Place place) {
		this.place = place;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Sorcerer sorcerer = (Sorcerer) o;

		return !(id != null ? !id.equals( sorcerer.id ) : sorcerer.id != null);

	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

}
