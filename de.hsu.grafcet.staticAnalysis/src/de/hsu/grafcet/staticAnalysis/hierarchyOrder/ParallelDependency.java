package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.util.LinkedHashSet;

public class ParallelDependency {
	
	LinkedHashSet<HierarchyDependency> dependencies = new LinkedHashSet<HierarchyDependency>();

	public LinkedHashSet<HierarchyDependency> getDependencies() {
		return dependencies;
	}

	public void addDependency(HierarchyDependency d) {
		dependencies.add(d);
	}
	
	
}
