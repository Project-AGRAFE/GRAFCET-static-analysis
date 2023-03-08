package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hsu.grafcet.InitializableType;
import de.hsu.grafcet.Step;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.VariableDeclaration;

public class HierarchyOrder {

	Hypergraf hypergraf;
//	LinkedHashSet<HierarchyDependency> sequentialDependencies = new LinkedHashSet<HierarchyDependency>();
//	LinkedHashSet<ParallelDependency> parallelDependencies = new LinkedHashSet<ParallelDependency>();
	LinkedHashSet<HierarchyDependency> dependencies = new LinkedHashSet<HierarchyDependency>();
//	Map<Vertex, Set<Vertex>> concurrentVertices; //zu rechenintensiv? tbd ob das Sinn macht?
	
//	public LinkedHashSet<HierarchyDependency> getSequentialDependencies() {
//		return sequentialDependencies;
//	}
//	public void setSequentialDependencies(LinkedHashSet<HierarchyDependency> sequentialDependencies) {
//		this.sequentialDependencies = sequentialDependencies;
//	}
//	public LinkedHashSet<ParallelDependency> getParallelDependencies() {
//		return parallelDependencies;
//	}
//	public void setParallelDependencies(LinkedHashSet<ParallelDependency> parallelDependencies) {
//		this.parallelDependencies = parallelDependencies;
//	}
	
	public LinkedHashSet<HierarchyDependency> getDependencies() {
		return dependencies;
	}
	public void setDependencies(LinkedHashSet<HierarchyDependency> dependencies) {
		this.dependencies = dependencies;
	}
	
	public Hypergraf getHypergraf() {
		return hypergraf;
	}
	public void setHypergraf(Hypergraf hypergraf) {
		this.hypergraf = hypergraf;
	}
	
	public HierarchyOrder() {
		super();
	}

//	public Map<Vertex, Set<Vertex>> getConcurrentVertices(){
//		if (concurrentVertices == null) {
//			calculateConcurrentVertices();
//		} 
//		return concurrentVertices;
//	}
	
	public Set<InitializableType> getConcurrentSteps(Vertex vertex){
		Set<Vertex> concurrentSteps = new LinkedHashSet<Vertex>();
		HierarchyDependency containingDependency = null;
		
		//FIXME vertex kann auch in mehrern dependencies sein!
		for (HierarchyDependency d:  dependencies) {
			if (d.getReachableVertices().contains(vertex)) {
				concurrentSteps.addAll(d.getConcurrentVertices().get(vertex));
				containingDependency = d;
			}
		}
		if (containingDependency == null) {
			throw new IllegalArgumentException("Vertex not included in Hierarchy-Order");
		}
		for (HierarchyDependency d : getConcurrentDependencies(containingDependency)) {
			concurrentSteps.addAll(d.getReachableVertices());
		}
		
		return null;
	}
	
	public Set<HierarchyDependency> getConcurrentDependencies(HierarchyDependency dependency){
		Set<HierarchyDependency> concurrentDependencies= new LinkedHashSet<HierarchyDependency>();
		
	}
	
	private void setParallelDependencies() {
//		for (HierarchyDependency dependency : dependencies) {
		List<HierarchyDependency> dependenciesCopy = new ArrayList<HierarchyDependency>(dependencies);
		while (!dependenciesCopy.isEmpty()) {
			ParallelDependency pd = new ParallelDependency();
			HierarchyDependency d = dependenciesCopy.get(0);
			dependenciesCopy.remove(0);
			for (HierarchyDependency d2 : dependenciesCopy) {
				if (isParallel(d, d2)) {
					pd.addDependency(d);
					pd.addDependency(d2);
				}
			}
		}		
	}
	
	private boolean isParallel(HierarchyDependency d1, HierarchyDependency d2) {
		for (HierarchyDependency d : dependencies) {
			if (d.getReachableVertices().contains(d1.getSuperiorTriggerVertex()) && d.getReachableVertices().contains(d2.getSuperiorTriggerVertex())) {
				if (d.getConcurrentVertices().get(d1.getSuperiorTriggerVertex()).contains(d2.getSuperiorTriggerVertex())) {
					return true;
				}
			}
		}
		return false;
	}
	
	
}
