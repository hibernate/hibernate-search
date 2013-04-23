package org.hibernate.search.test.integration.jtaspring;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.NaturalId;

@MappedSuperclass
public abstract class AbstractEntity {

	@NaturalId(mutable=true) 
	@Column(length=36)
	private String magicKey_;
	
	/**
	 * Sets the magic key for the instance
	 */
	public AbstractEntity() {
		magicKey_ = UUID.randomUUID().toString();
	}

	/**
	 * @return the magicKey
	 */
	public String getMagicKey() {
		return magicKey_;
	}

	/**
	 * @param magicKey the magicKey to set
	 */
	public void setMagicKey(String magicKey) {
		magicKey_ = magicKey;
	}
}
