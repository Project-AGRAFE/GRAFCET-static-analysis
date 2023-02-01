package de.hsu.grafcet.staticAnalysis.hypergraf;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.staticAnalysis.hierarchyOrder.ISubgraf;
import de.hsu.grafcet.*;

public class Subgraf implements ISubgraf{

	LinkedHashSet<Vertex> vertices = new LinkedHashSet<Vertex>();
	LinkedHashSet<Edge> edges = new LinkedHashSet<Edge>();
	PartialGrafcet partialGrafcet;
	int hierarchyDepth;
	private LinkedHashSet<Vertex> initialVertices = null;
	private LinkedHashSet<Edge> sourceEdges = null;
	
	/**
	 * REQUIRE: all (initial) vertices have been added to the subgraf
	 * @return
	 */
	public LinkedHashSet<Vertex> getInitialVertices(){
		if (initialVertices == null) {
			initialVertices = new LinkedHashSet<Vertex>();
			for (Vertex vertex : vertices) {
				if (vertex.getStep() instanceof InitializableType) {
					if (((InitializableType)vertex.getStep()).isInitial()) {
						initialVertices.add(vertex);
					}
				}
			}
		}
		return initialVertices;
	}
	
	/**
	 * REQUIRE: all source edges have been added to the subgraf
	 * REQUIRE: all other edges have been added completely (i.e. no edges with missing upstream vertices exists)
	 * @return
	 */
	public LinkedHashSet<Edge> getSourceEdges(){
		if (sourceEdges == null) {
			sourceEdges = new LinkedHashSet<Edge>();
			for (Edge edge : edges) {
				if (edge.getUpstream().isEmpty()) {
					sourceEdges.add(edge);
				}
			}
		}
		return sourceEdges;
	}
	
	
	public Subgraf(PartialGrafcet partialGrafcet) {
		super();
		this.partialGrafcet = partialGrafcet;
	}

	public Set<Edge> getUpstreamEdges(Vertex v){
		LinkedHashSet<Edge> upstreamEdges = new LinkedHashSet<Edge>();
		for (Edge e : edges) {
			if (e.getDownstream().contains(v)) {
				upstreamEdges.add(e);
			}
		}
		return upstreamEdges;
	}
	
	public Set<Edge> getDownstreamEdges(Vertex v){
		LinkedHashSet<Edge> downstreamEdges = new LinkedHashSet<Edge>();
		for (Edge e : edges) {
			if (e.getUpstream().contains(v)) {
				downstreamEdges.add(e);
			}
		}
		return downstreamEdges;
	}
	
	
	public LinkedHashSet<Vertex> getVertices() {
		return vertices;
	}
	public void setVertices(LinkedHashSet<Vertex> vertices) {
		this.vertices = vertices;
	}
	public LinkedHashSet<Edge> getEdges() {
		return edges;
	}
	public void setEdges(LinkedHashSet<Edge> edges) {
		this.edges = edges;
	}

	public PartialGrafcet getPartialGrafcet() {
		return partialGrafcet;
	}

	public void setPartialGrafcet(PartialGrafcet partialGrafcet) {
		this.partialGrafcet = partialGrafcet;
	}
	
	/**
	 * @param step
	 * @param subgraf
	 * @return
	 */
	public Vertex getVertexFromStep(Node step) {
		if (step instanceof Step ||
				step instanceof EnclosingStep ||
				step instanceof EntryStep ||
				step instanceof ExitStep) {
			for (Vertex v : this.getVertices()) {
				if (v.getStep() == step) {
					return v;
				}
			}
		} else {
			throw new IllegalArgumentException("Step must be of type Step, EclosingStep, EntryStep or ExitStep.");
		}	
		return null;
	}

	public Collection<? extends Statement> getActivationLinkVertices() {
		Set<Vertex> activationLinkvertices = new LinkedHashSet<Vertex>();
		for(InitializableType initType : this.getPartialGrafcet().getSteps()) {
			if (initType.isActivationLink()) {
				activationLinkvertices.add(this.getVertexFromStep(initType));
			}
		}
		return activationLinkvertices;
	}
	

	
	
}
