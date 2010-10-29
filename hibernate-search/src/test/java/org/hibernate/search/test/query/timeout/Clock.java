package org.hibernate.search.test.query.timeout;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Clock {
	public Clock() {}
	
	public Clock(String model, String brand, Long durability) {
		this.model = model;
		this.brand = brand;
		this.durability = durability;
	}

	@Id
	@GeneratedValue
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;

	@Field
	public String getModel() { return model; }
	public void setModel(String model) {  this.model = model; }
	private String model;

	@Field
	public String getBrand() { return brand; }
	public void setBrand(String brand) {  this.brand = brand; }
	private String brand;

	@Field
	public Long getDurability() { return durability; }
	public void setDurability(Long durability) {  this.durability = durability; }
	private Long durability;
}
