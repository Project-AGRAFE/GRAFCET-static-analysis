package de.hsu.grafcet.staticAnalysis.verification;

import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;

public abstract class Detecter {

	protected HierarchyOrder hierarchyOrder;
	protected Manager man;
	protected Environment env;
	
	public Detecter(HierarchyOrder h) {
		hierarchyOrder = h;
		env = hierarchyOrder.getApronEnvironment();
		man = new Box();
	}
	
	public abstract void runAnalysis() throws ApronException;
	
	public abstract String getResults();
	
}
