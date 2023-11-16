package de.hsu.grafcet.staticAnalysis.structuralAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import apron.NotImplementedException;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;

public class ExtendedStructuralConcurrencyAnalyzer {

	public static Map<Statement, Set<Statement>> calculateConcurrentStatementsForDependency(HierarchyDependency dependency) {
		
		Map<Vertex, Set<Edge>> concurrentVerticeEdgesMap = getConcurrentVericeEdgesMap(dependency); //TODO fehler
		Map<Vertex, Set<Statement>> concurrentVerticeStatementsMap = getConcurrentVerticeStatementsMap(dependency.getConcurrentVerticeVerticesMap(), 
				concurrentVerticeEdgesMap);
		Map<Edge, Set<Statement>> concurrentEdgeStatementsMap = getConcurrentEdgesStatementsMap(concurrentVerticeStatementsMap, dependency);
		
		Map<Statement, Set<Statement>> concurrentStatementStatementsMap = new HashMap<Statement, Set<Statement>>();
		concurrentStatementStatementsMap.putAll(concurrentVerticeStatementsMap);
		concurrentStatementStatementsMap.putAll(concurrentEdgeStatementsMap);
		
		return concurrentStatementStatementsMap;
	}
	
	public  static Map<Vertex, Set<Edge>> getConcurrentVericeEdgesMap(HierarchyDependency dependency) {
		 Map<Vertex, Set<Edge>> concurrentEdgesMap= new HashMap<Vertex, Set<Edge>>();
		 Map<Vertex, Set<Vertex>> concurrentVericesMap = dependency.getConcurrentVerticeVerticesMap();
		 Subgraf subgraf = dependency.getInferior();
		 for (Vertex vertex : concurrentVericesMap.keySet()) {
			 Set<Edge> concurrentEdges = new LinkedHashSet<Edge>();
			 
			 for (Vertex concurrentVertex : concurrentVericesMap.get(vertex)) {
				 concurrentEdges.addAll(subgraf.getUpstreamEdges(concurrentVertex));
				 concurrentEdges.addAll(subgraf.getDownstreamEdges(concurrentVertex));
				 
				 concurrentEdges.removeAll(subgraf.getUpstreamEdges(vertex));
				 concurrentEdges.removeAll(subgraf.getDownstreamEdges(vertex));
			 }
			 concurrentEdgesMap.put(vertex, concurrentEdges);
		 }
		 return concurrentEdgesMap;
	}
	
	public static Map<Vertex, Set<Statement>> getConcurrentVerticeStatementsMap(Map<Vertex, Set<Vertex>> concurrentVerticesMap, 
			Map<Vertex, Set<Edge>> concurrentEdgesMap){
		Map<Vertex, Set<Statement>> concurrentStatementsMap= new HashMap<Vertex, Set<Statement>>();
		
		for (Vertex vertex : concurrentVerticesMap.keySet()) {
			Set<Statement> concurrentStatements= new LinkedHashSet<Statement>();
			concurrentStatements.addAll(concurrentVerticesMap.get(vertex));
			concurrentStatements.addAll(concurrentEdgesMap.get(vertex));
			concurrentStatementsMap.put(vertex, concurrentStatements);
		}
		return concurrentStatementsMap;
	}
	
	public  static Map<Edge, Set<Statement>> getConcurrentEdgesStatementsMap(Map<Vertex, Set<Statement>> concurrentStatementsFromVerticesMap,
			HierarchyDependency dependency){
		Map<Edge, Set<Statement>> concurrentStatementsMap= new HashMap<Edge, Set<Statement>>();
		Set<Edge> edges = dependency.getInferior().getEdges();
		for (Edge edge : edges) {
			Vertex randomUpstreamVertex = new ArrayList<Vertex>(edge.getUpstream()).get(0);
			Set<Statement> concurrentUpstreamStatements;
			if (concurrentStatementsFromVerticesMap.get(randomUpstreamVertex) != null) {
				concurrentUpstreamStatements = 
						new  LinkedHashSet<Statement>(concurrentStatementsFromVerticesMap.get(randomUpstreamVertex));
			} else {
				concurrentUpstreamStatements = new  LinkedHashSet<Statement>();
			}

			for (Vertex upstreamVertex : edge.getUpstream()) {
				if(concurrentStatementsFromVerticesMap.get(upstreamVertex) != null) {
					concurrentUpstreamStatements.retainAll(concurrentStatementsFromVerticesMap.get(upstreamVertex));
				}
			}
			
			Vertex randomDownstreamVertex = new ArrayList<Vertex>(edge.getDownstream()).get(0);
			Set<Statement> concurrentDownstreamStatements;
			if (concurrentStatementsFromVerticesMap.get(randomDownstreamVertex) != null) {
				concurrentDownstreamStatements = 
						new  LinkedHashSet<Statement>(concurrentStatementsFromVerticesMap.get(randomDownstreamVertex));
			} else {
				concurrentDownstreamStatements = new  LinkedHashSet<Statement>();
				
			}
			for (Vertex downstreamVertex : edge.getUpstream()) {
				if (concurrentStatementsFromVerticesMap.get(downstreamVertex) != null) {
					concurrentDownstreamStatements.retainAll(concurrentStatementsFromVerticesMap.get(downstreamVertex));
				}
			}
			Set<Statement> concurrentStatements = new LinkedHashSet<Statement>(concurrentDownstreamStatements);
			concurrentStatements.addAll(concurrentUpstreamStatements);
			concurrentStatementsMap.put(edge, concurrentStatements);
		}
		return concurrentStatementsMap;
	}
	

}
