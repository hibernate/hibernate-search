package org.hibernate.search.test.service;

/**
 * @author Emmanuel Bernard
 */
public class ProvidedService {
	private final boolean provided;

	public ProvidedService(boolean provided) {
		this.provided = provided;
	}

	public ProvidedService() {
		this.provided = false;
	}

	public boolean isProvided() {
		return provided;
	}
}
