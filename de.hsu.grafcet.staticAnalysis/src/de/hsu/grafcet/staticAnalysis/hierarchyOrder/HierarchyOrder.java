package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.util.LinkedHashSet;

import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;

public class HierarchyOrder {

	Hypergraf hypergraf;
	LinkedHashSet<HierarchyDependency> sequentialDependencies = new LinkedHashSet<HierarchyDependency>();
	LinkedHashSet<ParallelDependency> parallelDependencies = new LinkedHashSet<ParallelDependency>();
	LinkedHashSet<HierarchyDependency> dependencies = new LinkedHashSet<HierarchyDependency>();
	
	public LinkedHashSet<HierarchyDependency> getSequentialDependencies() {
		return sequentialDependencies;
	}
	public void setSequentialDependencies(LinkedHashSet<HierarchyDependency> sequentialDependencies) {
		this.sequentialDependencies = sequentialDependencies;
	}
	public LinkedHashSet<ParallelDependency> getParallelDependencies() {
		return parallelDependencies;
	}
	public void setParallelDependencies(LinkedHashSet<ParallelDependency> parallelDependencies) {
		this.parallelDependencies = parallelDependencies;
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
	
	public HierarchyOrder() {
		super();
	}

	
	
}
