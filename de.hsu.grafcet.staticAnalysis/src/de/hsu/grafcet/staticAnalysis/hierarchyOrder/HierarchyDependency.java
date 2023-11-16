package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.hsu.grafcet.ForcingOrder;
import de.hsu.grafcet.InitializableType;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.ExtendedStructuralConcurrencyAnalyzer;


/**
 * 
 * @author Schnakenbeck
 * @
 */
public class HierarchyDependency{


	private Vertex superiorTriggerVertex;	
	private Set<Vertex> reachableVertices = new LinkedHashSet<Vertex>();
	private Set<Vertex> liveVertices = new LinkedHashSet<Vertex>();
	private Map<Vertex, Set<Vertex>> concurrentVerticeVerticesMap = new LinkedHashMap<Vertex, Set<Vertex>>();
//	private Map<Vertex, Set<Statement>> concurrentVerticeStatementsMap = new LinkedHashMap<Vertex, Set<Statement>>();
	private Map<Statement, Set<Statement>> concurrentStatementStatementsMap = null;
	private ISubgraf superior;
	private Subgraf inferior;
	private String inferiorName;
	private InitializationType type;
	private Set<Vertex> initiallyActiveVertices = null;  //initial Step in case of type initialStep; superior enclosingStep in case of type enclosed; superior step corresponding to forcingOrder in case of type forced; null in case of type sourceTransition
	
	
	public HierarchyDependency(Vertex superiorTriggerVertex,
			ISubgraf superior, Subgraf inferior, InitializationType type, Set<Vertex> initiallyActiveVertices ) {
		this.superiorTriggerVertex = superiorTriggerVertex;
		this.superior = superior;
		this.inferior = inferior;
		this.inferiorName = inferior.getPartialGrafcet().getName();
		this.type = type;
		this.initiallyActiveVertices = initiallyActiveVertices;
	}

	public Vertex getSuperiorTriggerVertex() {
		return superiorTriggerVertex;
	}

	public void setSuperiorTriggerVertex(Vertex superiorTriggerVertex) {
		this.superiorTriggerVertex = superiorTriggerVertex;
	}

	public Set<Vertex> getReachableVertices() {
		return reachableVertices;
	}

	public void setReachableVertices(Set<Vertex> reachableVertices) {
		this.reachableVertices = reachableVertices;
	}

	public Set<Vertex> getLiveVertices() {
		return liveVertices;
	}

	public void setLiveVertices(LinkedHashSet<Vertex> liveVertices) {
		this.liveVertices = liveVertices;
	}

	public Map<Vertex, Set<Vertex>> getConcurrentVerticeVerticesMap() {
		return concurrentVerticeVerticesMap;
	}
	
	public Set<Vertex> getConcurrentVertices(Vertex v){
		return concurrentVerticeVerticesMap.get(v);
	}

//	public void setConcurrentVertices(Map<Vertex, Set<Vertex>> concurrentVertices) {
//		this.concurrentVerticeVerticesMap = concurrentVertices;
//	}

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
	/**
	 * @deprecated weil Methode unten deprecated
	 */
	public Set<Vertex> getInitiallyActiveVertices() {
		if(initiallyActiveVertices == null) {
			setInitiallyActiveVertices();
		}
		return initiallyActiveVertices;
	}
	
	/**
	 * @deprecated calcularion of concurrent Transitions is not working
	 */
	public void setConcurrentStatementStatementsMap() {
		this.concurrentStatementStatementsMap = ExtendedStructuralConcurrencyAnalyzer.calculateConcurrentStatementsForDependency(this);
	}
	
	/**
	 * @deprecated calcularion of concurrent Transitions is not working
	 */
	public Map<Statement, Set<Statement>> getConcurrentStatementStatementsMap() {
		if(concurrentStatementStatementsMap == null) {
			setConcurrentStatementStatementsMap();
		}
		return concurrentStatementStatementsMap;
	}
	

	/**
	 * @deprecated
	 */
	private void setInitiallyActiveVertices() {
		initiallyActiveVertices = new HashSet<Vertex>();
		switch (this.getType()) {
		case initialStep: {
			initiallyActiveVertices.addAll(this.inferior.getInitialVertices());
			break;
		} case sourceTransition: {
			//do nothing, sourceTranstitions will be considered anyways
			break;
		} case enclosed: {
			for (Vertex vertex : this.inferior.getVertices()) {
				if (vertex.getStep() instanceof InitializableType) {
					if (((InitializableType) vertex.getStep()).isActivationLink()) {
						initiallyActiveVertices.add(vertex);
					}
				}
			}
			break;
		} case forced: {	
			switch (((ForcingOrder)this.getSuperiorTriggerVertex()).getForcingOrderType()) {
			case CURRENT_SITUATION: {
				//abort analysis; analysis covered with one of the other possibilities
				break;
			} case EMPTY_SITUATION: {
				//no steps reachable; except sourceTransitions are set
				break;
			} case EXPLICIT_SITUATION: {
				for (InitializableType step : ((ForcingOrder)this.getSuperiorTriggerVertex()).getForcedSteps()){
					initiallyActiveVertices.add(this.getInferior().getVertexFromStep(step));
				}
				break;
			} case INITIAL_SITUATION: {
				initiallyActiveVertices.addAll(this.getInferior().getInitialVertices());
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + ((ForcingOrder)this.getSuperiorTriggerVertex()).getForcingOrderType());
			}
			break;
		}
		default: 
			throw new IllegalArgumentException("Unexpected value: " + this.getType());
		}
	}
	
	public String getInferiorName() {
		return inferiorName;
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
		if (!this.getConcurrentVerticeVerticesMap().containsKey(annotatedVertex)) {
			this.getConcurrentVerticeVerticesMap().put(annotatedVertex, new LinkedHashSet<Vertex>());
		}
		//add step
		this.getConcurrentVerticeVerticesMap().get(annotatedVertex).add(concurrentVertex);
	}

	@Override
	public String toString() {
		String txt = inferior.getPartialGrafcet().getName() + " - with initialization type " + type + ": \n";
		txt +=  "reachable steps: " + reachableVertices + "\n";
		txt += "concurrent steps: \n";
		for (Vertex vertex : concurrentVerticeVerticesMap.keySet()) {
			txt += vertex + ": " + concurrentVerticeVerticesMap.get(vertex) + "\n";
		}
		
		getConcurrentStatementStatementsMap(); //trigger calculation
		txt += "concurrent statements: \n";
		for (Statement statement: concurrentStatementStatementsMap.keySet()) {
			txt += statement + ": " + concurrentVerticeVerticesMap.get(statement) + "\n";
		}
		return txt;
	}

	
	
	
}
