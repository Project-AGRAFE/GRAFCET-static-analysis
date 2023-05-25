package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import apron.ApronException;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.InitializationType;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;

public class HierarchicalConflictDetecter extends Detecter{	

	String result = "\n\n== Results HierarchicalConflictDetecter: ==";
	
	public HierarchicalConflictDetecter(HierarchyOrder hierarchyOrder) {
		super(hierarchyOrder);
	}
	
	@Override
	public void runAnalysis() throws ApronException {
		runHierarchyConditionDetecter();
	}

	@Override
	public String getResults() {
		return result;
	}
	
	private void runHierarchyConditionDetecter() {

		//collect actions:
		List<HierarchyDependency> forcingsAndEnclosings = new ArrayList<HierarchyDependency>();
		for (HierarchyDependency d : hierarchyOrder.getDependencies()) {
			if (d.getType() == InitializationType.enclosed || d.getType() == InitializationType.forced) {
				forcingsAndEnclosings.add(d);
			}
		}
		//collect hierarchies that writhe the same partial Grafcet:
		Set<Set<HierarchyDependency>> setsOfConcurrentHierarchies = new HashSet<Set<HierarchyDependency>>();
		while (!forcingsAndEnclosings.isEmpty()) {
			Set<HierarchyDependency> concurrentHierarchies = new HashSet<HierarchyDependency>();
			Subgraf inferiorSubgraf = forcingsAndEnclosings.get(0).getInferior();
			for (HierarchyDependency d : forcingsAndEnclosings) {
				if (inferiorSubgraf.equals(d.getInferior())) {
					concurrentHierarchies.add(d);
				}
			}
			forcingsAndEnclosings.removeAll(concurrentHierarchies);
			if (concurrentHierarchies.size() > 1) {
				setsOfConcurrentHierarchies.add(concurrentHierarchies);
			}
		}		
		//check if superior trigger vertices can be concurrent:
		Set<List<HierarchyDependency>> setsOfConcurrentPairs = new HashSet<List<HierarchyDependency>>();
		for (Set<HierarchyDependency> concurrentHierarchies : setsOfConcurrentHierarchies) {
			for (List<HierarchyDependency> pair : Util.getSetOfPairs(concurrentHierarchies)) {
				if (hierarchyOrder.getConcurrentSteps(pair.get(0).getSuperiorTriggerVertex()).contains(pair.get(1).getSuperiorTriggerVertex()) ) { 
					setsOfConcurrentPairs.add(pair);
				}			
			}
		}
		
		if (setsOfConcurrentPairs.isEmpty()) {
			result += "\nNo race conditions detected";
		} else {
			String out = "\nThe following conflicts between hierarchies were detected: \n";
			for (List<HierarchyDependency> pair : setsOfConcurrentPairs) {
				out += "Hierarchy " + pair.get(0).getType() + " associated to step " + pair.get(0).getSuperiorTriggerVertex() + 
						" is in conflict with " + pair.get(1).getType() + " associated to step " + pair.get(1).getSuperiorTriggerVertex() 
						+ "\n";
			}
			result += out;
		}
	}
}
