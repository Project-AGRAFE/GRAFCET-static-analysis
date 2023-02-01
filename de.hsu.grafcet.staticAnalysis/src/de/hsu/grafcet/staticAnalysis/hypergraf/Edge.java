package de.hsu.grafcet.staticAnalysis.hypergraf;

import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.Transition;

public class Edge implements Statement{
	
	private LinkedHashSet<Vertex> upstream = new LinkedHashSet<Vertex>();
	private LinkedHashSet<Vertex> downstream = new LinkedHashSet<Vertex>();
	private int id;
	private Transition transition;
	private int visited = 0;
	
	
	public Edge(Transition transition) {
		this.id = transition.getId();
		this.transition = transition;
	}
	public LinkedHashSet<Vertex> getUpstream() {
		return upstream;
	}
	public void setUpstream(LinkedHashSet<Vertex> upstream) {
		this.upstream = upstream;
	}
	public Set<Vertex> getDownstream() {
		return downstream;
	}
	public void setDownstream(LinkedHashSet<Vertex> downstream) {
		this.downstream = downstream;
	}
	public Transition getTransition() {
		return transition;
	}
	
	@Override
	public int getId() {
		return id;
	}
	@Override
	public int getVisited() {
		return visited;
	}
	@Override
	public void increaseVisited() {
		visited++;
		
	}
	@Override
	public void resetVisited() {
		visited = 0;
		
	}
	@Override
	public String toString() {
		return "t" + id;
	}
}
