package org.hibernate.search.jsr352.test.entity;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;


/**
 * The persistent class for the stock database table.
 * 
 */
@Entity
@NamedQuery(name="Stock.findAll", query="SELECT s FROM Stock s")
@Table(name="stock")
public class Stock implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;

	@Column(name="adj_close")
	private float adjClose;

	private float close;

	private String company;

	@Temporal(TemporalType.DATE)
	private Date date;

	private float high;

	private float low;

	private float open;

	private int volume;

	public Stock() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public float getAdjClose() {
		return this.adjClose;
	}

	public void setAdjClose(float adjClose) {
		this.adjClose = adjClose;
	}

	public float getClose() {
		return this.close;
	}

	public void setClose(float close) {
		this.close = close;
	}

	public String getCompany() {
		return this.company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public Date getDate() {
		return this.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public float getHigh() {
		return this.high;
	}

	public void setHigh(float high) {
		this.high = high;
	}

	public float getLow() {
		return this.low;
	}

	public void setLow(float low) {
		this.low = low;
	}

	public float getOpen() {
		return this.open;
	}

	public void setOpen(float open) {
		this.open = open;
	}

	public int getVolume() {
		return this.volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

    @Override
    public String toString() {
        return "Stock [id=" + id + ", adjClose=" + adjClose + ", close=" + close
                + ", company=" + company + ", date=" + date + ", high=" + high
                + ", low=" + low + ", open=" + open + ", volume=" + volume
                + "]";
    }
}