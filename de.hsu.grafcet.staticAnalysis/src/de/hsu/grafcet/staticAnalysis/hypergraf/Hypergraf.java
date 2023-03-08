package de.hsu.grafcet.staticAnalysis.hypergraf;

import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.staticAnalysis.hierarchyOrder.*;
import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.PartialGrafcet;
import de.hsu.grafcet.Step;

public class Hypergraf implements ISubgraf {

	private LinkedHashSet<Subgraf> subgrafs = new LinkedHashSet<Subgraf>();
	private Grafcet globalGrafcet;
	
	public Hypergraf(Grafcet globalGrafcet) {
		this.globalGrafcet = globalGrafcet;
	}
	
	public Hypergraf(LinkedHashSet<Subgraf> sg, Grafcet globalGrafcet) {
		this(globalGrafcet);
		this.subgrafs.addAll(sg);
	}

	public Grafcet getGlobalGrafcet() {
		return globalGrafcet;
	}
	
	public LinkedHashSet<Subgraf> getSubgrafs() {
		return subgrafs;
	}

	public void setSubgrafs(LinkedHashSet<Subgraf> subgrafs) {
		this.subgrafs = subgrafs;
	}
	
	public Subgraf getSubgraf(PartialGrafcet pg) {
		Subgraf sgout = null;
		boolean found = false;
		for (Subgraf sg : this.getSubgrafs()) {
			if (sg.getPartialGrafcet() != null) {
				
				if (sg.getPartialGrafcet().equals(pg)) {
					if (found) {
						throw new IllegalArgumentException("Partial Grafcet " + pg.getName() + " has two corresponding subgrafs in hypergraf.");
					} else {
						found = true;
						sgout = sg;		
					}
				}
			}
		}
		if (sgout == null) {
			throw new IllegalArgumentException("Partial Grafcet " + pg.getName() + " has no corresponding subgraf in hypergraf.");
		} else {
			return sgout;
		}
	}
	
	public boolean isSequential() {
		if (this.getSubgrafs().size() > 1) {
			//TODO muss genauer berechnet werden. Nur bei abhängigen und parallel laufenden Teilgrafcet ist Bedingung verletzt
			return false;
		}
		for (Subgraf subgraf : this.getSubgrafs()) {
				if (!subgraf.isSequential()) {
					return false;
				}
		}
		return true;
	}

	public Vertex getVertex(Step step) {
		for (Subgraf sg : subgrafs) {
			for (Vertex vertex : sg.getVertices()) {
				if (vertex.getStep().equals(step)) {
					return vertex;
				}
			}
		}
		return null;
	}
	
	public Set<Edge> getUpstreamEdges(Vertex v){
		for (Subgraf sg : subgrafs) {
			if (sg.getVertices().contains(v)) {
				return sg.getUpstreamEdges(v);
			}
		}
		return null;
	}
	
	public Set<Edge> getDownstreamEdges(Vertex v){
		for (Subgraf sg : subgrafs) {
			if (sg.getVertices().contains(v)) {
				return sg.getDownstreamEdges(v);
			}
		}
		return null;
	}
	
}
