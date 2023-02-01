package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.staticAnalysis.hypergraf.*;


/**
 * 
 * @author Schnakenbeck
 * @
 */
public class HierarchyDependency{
	
	/**
	 //TODO bei initialschritten kann es auch eine Menge sein!!
	 * initial Step in case of type initialStep; superior enclosingStep in case of type enclosed; superior step corresponding to forcingOrder in case of type forced; null in case of type sourceTransition
	 */

	Vertex superiorTriggerVertex;	
	LinkedHashSet<Vertex> reachableVertices = new LinkedHashSet<Vertex>();
	LinkedHashSet<Vertex> liveVertices = new LinkedHashSet<Vertex>();
	LinkedHashMap<Vertex, LinkedHashSet<Vertex>> concurrentVertices = new LinkedHashMap<Vertex, LinkedHashSet<Vertex>>();
	ISubgraf superior;
	Subgraf inferior;
	InitializationType type;
	
	public HierarchyDependency(Vertex superiorTriggerVertex,
			ISubgraf superior, Subgraf inferior, InitializationType type) {
		this.superiorTriggerVertex = superiorTriggerVertex;
		this.superior = superior;
		this.inferior = inferior;
		this.type = type;
	}

	public Vertex getSuperiorTriggerVertex() {
		return superiorTriggerVertex;
	}

	public void setSuperiorTriggerVertex(Vertex superiorTriggerVertex) {
		this.superiorTriggerVertex = superiorTriggerVertex;
	}

	public LinkedHashSet<Vertex> getReachableVertices() {
		return reachableVertices;
	}

	public void setReachableVertices(LinkedHashSet<Vertex> reachableVertices) {
		this.reachableVertices = reachableVertices;
	}

	public LinkedHashSet<Vertex> getLiveVertices() {
		return liveVertices;
	}

	public void setLiveVertices(LinkedHashSet<Vertex> liveVertices) {
		this.liveVertices = liveVertices;
	}

	public LinkedHashMap<Vertex, LinkedHashSet<Vertex>> getConcurrentVertices() {
		return concurrentVertices;
	}

	public void setConcurrentVertices(LinkedHashMap<Vertex, LinkedHashSet<Vertex>> concurrentVertices) {
		this.concurrentVertices = concurrentVertices;
	}

	public ISubgraf getSuperior() {
		return superior;
	}

	public void setSuperior(Subgraf superior) {
		this.superior = superior;
	}

	public Subgraf getInferior() {
		return inferior;
	}

	public void setInferior(Subgraf inferior) {
		this.inferior = inferior;
	}

	public InitializationType getType() {
		return type;
	}

	public void setType(InitializationType type) {
		this.type = type;
	}

	
	public void addConcurrentVertices (Vertex annotatedVertex, Set<Vertex> concurrentVertices) {
		for (Vertex concurrentVertex : concurrentVertices) {
			addConcurrentVertex(annotatedVertex, concurrentVertex);
		}
	}
	public void addConcurrentVertex (Vertex annotatedVertex, Vertex concurrentVertex) {
		//list of concurrent steps should not contain the step itself
		if (annotatedVertex.equals(concurrentVertex)) {
			return;
		}
		//if list of concurrent steps is not created yet:
		if (!this.getConcurrentVertices().containsKey(annotatedVertex)) {
			this.getConcurrentVertices().put(annotatedVertex, new LinkedHashSet<Vertex>());
		}
		//add step
		this.getConcurrentVertices().get(annotatedVertex).add(concurrentVertex);
	}

	@Override
	public String toString() {
		String txt = inferior.getPartialGrafcet().getName() + " - with initialization type " + type + ": \n";
		txt +=  "reachable steps: " + reachableVertices + "\n";
		txt += "concurrent steps: \n";
		for (Vertex vertex : concurrentVertices.keySet()) {
			txt += vertex + ": " + concurrentVertices.get(vertex) + "\n";
		}
		return txt;
	}
	
	
	
	
}
