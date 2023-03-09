package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.InitializableType;
import de.hsu.grafcet.Step;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import de.hsu.grafcet.staticAnalysis.hypergrafTransromation.HypergrafcetGenerator;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.StructuralConcurrencyAnalyzer;
import terms.VariableDeclaration;

public class HierarchyOrder {

	private Hypergraf hypergraf;
//	LinkedHashSet<HierarchyDependency> sequentialDependencies = new LinkedHashSet<HierarchyDependency>();
	private LinkedHashSet<ParallelDependency> parallelDependencies;
	private LinkedHashSet<HierarchyDependency> dependencies = new LinkedHashSet<HierarchyDependency>();
//	Map<Vertex, Set<Vertex>> concurrentVertices; //zu rechenintensiv? tbd ob das Sinn macht?

	
	public HierarchyOrder(Grafcet grafcet) {
		super();
		HypergrafcetGenerator hg = new HypergrafcetGenerator(grafcet, this);
		hg.generate();		
	}	
	
	
	
	/**
	 * check HierarchOrder according to Lesage et al.:
	 * @return
	 */
	public String analyzeHierarchyOrderForPartialOrder() {
		//check for cycles
		//calculate depth
		return new String("Result of hierarchy check for partial order");
	}
	
	public String analyzeReachabilityAndConcurrency() {
		String out = "";
		for (HierarchyDependency dependency : dependencies) {
			out += "Partial Grafcet " + dependency.getInferior() + "\n Dependency: ";
			out += StructuralConcurrencyAnalyzer.reachabilityConcurrencyAnalysis(dependency, false, true);
		}
		return out;
	}
	
	public void analyzeRecaConditions() {
		
	}
	public void analyzeDeadlocks() {
		
	}
	
	
	
	
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

	
	/**
	 * returns all concurrent vertices to @param vertex. 
	 * Concurrent vertices are: 
	 * - concurrent vertices from the same subgraf
	 * - reachable vertices from parallel hierarchical dependencies (parallelDependencies are used for calculation, as well as a depth search of inferior dependencies)
	 * Concurrent vertices are NOT:
	 * - superior trigger vertices
	 * - reachable steps from inferior dependencies induces by @param vertex
	 * @param vertex
	 * @return
	 */
	
	public Set<Vertex> getConcurrentSteps(Vertex vertex){
		if (parallelDependencies == null) {
			setParallelDependencies();
		}
		Set<Vertex> concurrentSteps = new LinkedHashSet<Vertex>();
		Set<HierarchyDependency> containingDependencies = new LinkedHashSet<HierarchyDependency>();
		Subgraf containingSubgraf = null;
		
		for (HierarchyDependency d:  dependencies) {
			if (d.getReachableVertices().contains(vertex)) {
				concurrentSteps.addAll(d.getConcurrentVertices().get(vertex));
				containingDependencies.add(d);
				//concurrentSteps.add(d.getSuperiorTriggerVertex());
				containingSubgraf = d.getInferior(); //sollte immer der selbe sein
			}
		}
		if (containingDependencies.isEmpty() || containingSubgraf == null) {
			throw new IllegalArgumentException("Vertex not included in Hierarchy-Order");
		}
		//get concurrent dependencies
		Set<HierarchyDependency> concurrentDependencies = new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency d : containingDependencies) {
			concurrentDependencies.addAll(getConcurrentDependencies(d));
		}
		//Remove all dependencies that have containingSubgraf as inferiorSubgraf 
		//This is the case of a "diamond" in the hierarchy order (which is a potential conflict)
		concurrentDependencies.removeAll(containingDependencies);
		
		for (HierarchyDependency concurrentDependency : concurrentDependencies) {
			concurrentSteps.addAll(concurrentDependency.getReachableVertices());
		}
		return concurrentSteps;
	}
	/**
	 * Runs a depth first seach on the hierarchy order starting with d and returns all visited hierarchy dependencies
	 * @param d
	 * @return
	 */
	

	
	private Set<HierarchyDependency> getConcurrentDependencies(HierarchyDependency dependency){
		Set<HierarchyDependency> concurrentDirectDependencies= new LinkedHashSet<HierarchyDependency>();
		
		//search for parallel dependencies
		for (ParallelDependency parallelDep : parallelDependencies) {
			if (parallelDep.getDependencies().contains(dependency)) {
				concurrentDirectDependencies.addAll(parallelDep.getDependencies());
				concurrentDirectDependencies.remove(dependency);
			}
		}
		
		//depth search
		Set<HierarchyDependency> concurrentInferiorDep= new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency concurrentDep : concurrentDirectDependencies) {
			concurrentInferiorDep.addAll(getAllInferiorDependencies(concurrentDep));
		}
		
		//get superior dependencies --> recursion
		Set<HierarchyDependency> concurrentSuperiorDependencies = new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency superiorDep : getSuperiorDependencies(dependency.getSuperior())) {
			concurrentSuperiorDependencies.addAll(getConcurrentDependencies(superiorDep));
		}
		
		//TODO: Es reicht ein Set aus, wenn alles funktioniert
		concurrentDirectDependencies.addAll(concurrentInferiorDep);
		concurrentDirectDependencies.addAll(concurrentSuperiorDependencies);
		return concurrentDirectDependencies;
	}
	
	private Set<HierarchyDependency> getAllInferiorDependencies(HierarchyDependency d){
		Set<HierarchyDependency> inferiorDependencies = new HashSet<HierarchyDependency>();
		Subgraf inferior = d.getInferior();
		List<HierarchyDependency> stack = new ArrayList<HierarchyDependency>();
		stack = getDirectInferiorDependencies(inferior);
		while (!stack.isEmpty()) {
			HierarchyDependency dep = stack.get(0);
			stack.remove(0);
			
			inferiorDependencies.addAll(getAllInferiorDependencies(dep));
		}
		return inferiorDependencies;
	}
	
	private List<HierarchyDependency> getDirectInferiorDependencies(Subgraf superior) {
		List<HierarchyDependency> inferiorDependencies = new ArrayList<HierarchyDependency>();
		for (HierarchyDependency d : dependencies) {
			if (d.getSuperior().equals(superior)) {
				inferiorDependencies.add(d);
			}
		}
		return inferiorDependencies;
	}
	
	private Set<HierarchyDependency> getSuperiorDependencies(ISubgraf inferior){
		Set<HierarchyDependency> superiorDependencies = new LinkedHashSet<HierarchyDependency>();
		if(inferior instanceof Hypergraf) {
			return superiorDependencies; //root of the hierarchy order
		} else {
			for (HierarchyDependency d : dependencies) {
				if (d.getInferior().equals(inferior)) {
					superiorDependencies.add(d);
				}
			}
			return superiorDependencies;
		}
	}
	
	private void setParallelDependencies() {
		parallelDependencies = new LinkedHashSet<ParallelDependency>();
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
			parallelDependencies.add(pd);
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
