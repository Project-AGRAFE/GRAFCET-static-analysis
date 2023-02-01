package de.hsu.grafcet.staticAnalysis.analysis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.staticAnalysis.hierarchyOrder.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import de.hsu.grafcet.*;

public class StructuralConcurrencyAnalyzer {


	
	
	/**
	 * Structural Analysis von GRAFCET i.e. Transition conditions are assumed to be true (i.e. a broad over-approximation)
	 * Analysis can analyze the structural reachability and if steps are structural concurrent.
	 * @param subgraf to be analyzed
	 * @param initialVertices depending on the InitializableType of the subgraf vertices corresponding to: Initial step(s), step(s) marked with activationLink, forced step(s); REQUIRE: algorithm must be performed FOR EACH of the mentioned sets
	 * @param sourceEdges edges corresponding to source transitions. REQUIRE: source edges must be mentioned regardless of the InitializationType
	 * @param analyzeLifeness boolean flag
	 * @param analyzeConcurrentSteps boolean flag
	 * @throws IllegalAccessException 
	 */
	
	public static String reachabilityConcurrencyAnalysis(HierarchyDependency dependency, boolean analyzeLifeness, boolean analyzeConcurrentSteps) {
		
		if (analyzeLifeness) {
			throw new IllegalArgumentException("liveness analysis not impemented yet");
		}
		
		Subgraf subgraf = dependency.getInferior();
		LinkedHashSet<Vertex> initialReachableVertices = new LinkedHashSet<Vertex>();
		
		//set initial vertices:
		switch (dependency.getType()) {
		case initialStep: {
			initialReachableVertices.addAll(subgraf.getInitialVertices());
			break;
		} case sourceTransition: {
			//do nothing, sourceTranstitions will be considered anyways
			break;
		} case enclosed: {
			for (Vertex vertex : subgraf.getVertices()) {
				if (vertex.getStep() instanceof InitializableType) {
					if (((InitializableType) vertex.getStep()).isActivationLink()) {
						initialReachableVertices.add(vertex);
					}
				}
			}
			
			break;
		} case forced: {	
			switch (((ForcingOrder)dependency.getSuperiorTriggerVertex()).getForcingOrderType()) {
			case CURRENT_SITUATION: {
				//abort analysis; analysis covered with one of the other possibilities
				return "abort analysis; analysis covered with one of the other possibilities";
			} case EMPTY_SITUATION: {
				//no steps reachable; except sourceTransitions are set
				break;
			} case EXPLICIT_SITUATION: {
				for (InitializableType step : ((ForcingOrder)dependency.getSuperiorTriggerVertex()).getForcedSteps()){
					initialReachableVertices.add(dependency.getInferior().getVertexFromStep(step));
				}
				break;
			} case INITIAL_SITUATION: {
				initialReachableVertices.addAll(subgraf.getInitialVertices());
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + ((ForcingOrder)dependency.getSuperiorTriggerVertex()).getForcingOrderType());
			}
			break;
		}
		default: 
			throw new IllegalArgumentException("Unexpected value: " + dependency.getType());
		}
		
		//Bedingugnen unter denen der Algorithmus potenziell nicht funktioniert:
		//zwei sequenziell parallele Stränge gefolgt von Synchronisation würde zu unterapproximation führen
		if (subgraf.getSourceEdges().size() > 2 || 
				(!subgraf.getSourceEdges().isEmpty() && initialReachableVertices.size() > 2) ||
				initialReachableVertices.size() > 4) {
			for (Vertex vertex : subgraf.getVertices()) {
				dependency.addConcurrentVertices(vertex, subgraf.getVertices());
			}
			return "Bedingugnen unter denen der Algorithmus potenziell nicht funktioniert: \n"
					+ "zwei sequenziell parallele Stränge gefolgt von Synchronisation würde zu unterapproximation führen";
		}
		
		
		CustomArrayDeque<Edge> worklist = new CustomArrayDeque<Edge>();
		
		
		//add sourceTransitions to worklist
		worklist.addAll(subgraf.getSourceEdges());
		//TODO: testen
		//make all reachable vertices from sourceTransitions concurrent to each other, because the the sourceTransition can activate the downstream steps an infinite amount of times
		if (!subgraf.getSourceEdges().isEmpty()) {
			reachabilityConcurrencyAlgorithm(worklist, dependency, false);
			for (Vertex vertex : dependency.getReachableVertices()) {
				dependency.addConcurrentVertices(vertex, dependency.getReachableVertices());
			}
		}
		
		
		
		
		worklist.addAll(subgraf.getSourceEdges());
		//Added //TODO test
		for (Edge sourceEdge : subgraf.getSourceEdges()) {
			//make downstream vertices of source transition concurrent to initial reachable steps
			for (Vertex downstreamVertex : sourceEdge.getDownstream()) {
				dependency.addConcurrentVertices(downstreamVertex, initialReachableVertices);
			}
			
		}
		for (Vertex vertex : initialReachableVertices) {	
			//make initial reachable vertices reachable
			dependency.getReachableVertices().add(vertex);
			//make initial reachable vertices concurrent
			dependency.addConcurrentVertices(vertex, initialReachableVertices);
			for (Edge sourceEdge : subgraf.getSourceEdges()) {
				dependency.addConcurrentVertices(vertex, sourceEdge.getDownstream());
			}
		}
		
		for (Vertex vertex : initialReachableVertices) {
			//add downstream transtions of initial reachable vertices to worklist
			for (Edge downstreamEdge : subgraf.getDownstreamEdges(vertex)) {
				if (isEnabled(downstreamEdge, subgraf.getSourceEdges(), dependency.getReachableVertices())){
					worklist.addAll(subgraf.getDownstreamEdges(vertex));
				}
			}			
		}
		
		//perform actual algorithm
		reachabilityConcurrencyAlgorithm(worklist, dependency, analyzeConcurrentSteps);
		return dependency.toString();
	}
	
	
	private static void reachabilityConcurrencyAlgorithm(CustomArrayDeque<Edge> worklist, HierarchyDependency dependency, boolean analyzeConcurrentSteps) {
		Subgraf subgraf = dependency.getInferior();
		while (!worklist.isEmpty()) {
			Edge edge = worklist.pollFirst(); //.pollFirst() entspricht Breitensuche, .pollLast() Tiefensuche. pollFirst() scheint schneller zu sein
			if (isEnabled(edge, subgraf.getSourceEdges(), dependency.getReachableVertices())) {									
				for(Vertex downstreamVertex : edge.getDownstream()) {	
					//reachable annotation:
					if (!dependency.getReachableVertices().contains(downstreamVertex)) {									
						dependency.getReachableVertices().add(downstreamVertex);
						worklist.addAll(subgraf.getDownstreamEdges(downstreamVertex));
					}
					if (analyzeConcurrentSteps) {
						annotateConcurrentVertices(dependency, downstreamVertex, edge);  //TODO: muss das erneut mit Liveness ausgef�hrt werden? Reicht es nicht dass bei jedem erreichbaren Schritt zu machen?
					}
				}
			}
		}
	}
	
	private static void annotateConcurrentVertices(HierarchyDependency dependency, Vertex downstreamVertex, Edge edge) {	
		dependency.addConcurrentVertices(downstreamVertex, edge.getDownstream());
		
		LinkedHashSet<Vertex> adoptedConcurrentVertices = new LinkedHashSet<Vertex>(); 
		for (Vertex upstreamVertex : edge.getUpstream()) {
			if (adoptedConcurrentVertices.isEmpty()) {
				if (dependency.getConcurrentVertices().get(upstreamVertex) != null) {
					adoptedConcurrentVertices.addAll(dependency.getConcurrentVertices().get(upstreamVertex));
				}
			} else {
				adoptedConcurrentVertices.retainAll(dependency.getConcurrentVertices().get(upstreamVertex));
			}
		}
		dependency.addConcurrentVertices(downstreamVertex, adoptedConcurrentVertices);
		for (Vertex adoptedConcurrentVertex : adoptedConcurrentVertices) {
			dependency.addConcurrentVertex(adoptedConcurrentVertex, downstreamVertex);
		}
	}
	
	private static boolean isEnabled(Edge edge, Collection<Edge> sourceEdges, Collection<Vertex> reachableSteps) {
		if (sourceEdges.contains(edge)) {
			return true;
		}
		for (Vertex upstreamVertex : edge.getUpstream()) {
			if (!reachableSteps.contains(upstreamVertex)) {
				return false;
			}
		}
		return true;
	}
	private static boolean isLiveEnabled(Edge edge, Collection<Edge> sourceEdges, Collection<Vertex> liveSteps) {
		if (sourceEdges.contains(edge)) {
			return true;
		}
		for (Vertex upstreamVertex : edge.getUpstream()) {
			if (!liveSteps.contains(upstreamVertex)) {
				return false;
			}
		}
		return true;
	}

}
