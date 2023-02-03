package de.hsu.grafcet.staticAnalysis.hypergrafTransromation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;

import apron.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.*;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.*;
import de.hsu.grafcet.*;

public class HypergrafcetGenerator {

	
	
	Grafcet grafcet;
	Hypergraf hypergraf;
	HierarchyOrder hierarchyOrder = new HierarchyOrder();
	
	
	public HypergrafcetGenerator(Grafcet grafcet) {
		super();
		this.grafcet = grafcet;
		hypergraf  = new Hypergraf(grafcet);
		//generate Hypergrafcet and HirarcyDependencies regarding enclosings and forcings (i.e. HierarchyOrder according to Lesage et al.):
		runInitialGeneration();

		//add ActionsTypes to Vertices
		addActionTypesToVertices();
		
		//extend HierarchyOrder for initialStep and sourceTransition dependencies:
		addInitialDependenciesToHierarchyOrder();
	}
	
	private void addActionTypesToVertices() {
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			for (ActionType actionType : subgraf.getPartialGrafcet().getActionTypes()) {
				for (ActionLink actionLink : subgraf.getPartialGrafcet().getActionLinks()) {
					if (actionLink.getActionType() == actionType) {
						if (actionType instanceof StoredAction) {
							if (((StoredAction)actionType).getStoredActionType() == StoredActionType.DEACTIVATION) {
								Vertex vertex = subgraf.getVertexFromStep(actionLink.getStep());
								Set<Edge> downstreamEdges = subgraf.getDownstreamEdges(vertex);
								for (Edge downstreamEdge : downstreamEdges) {
									new ArrayList<>(downstreamEdge.getDownstream()).get(0).addActionType(actionType);
								}
							}else {
								subgraf.getVertexFromStep(actionLink.getStep()).addActionType(actionType);
							}
						} else {
							subgraf.getVertexFromStep(actionLink.getStep()).addActionType(actionType);
						}
					}
				}
			}
		}
	}
	
	public HierarchyOrder returnHierarchyOrder() {
		hierarchyOrder.setHypergraf(hypergraf);
		return  hierarchyOrder;
	}
	

	
	
	
	
	
	
	/**
	 * check HierarchOrder according to Lesage et al.:
	 * @return
	 */
	public String checkHierarchyOrderForPartialOrder() {
		//check for cycles
		//calculate depth
		return new String("Result of hierarchy check for partial order");
	}
	
	
	
	
	private void addInitialDependenciesToHierarchyOrder() {
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			if (!subgraf.getInitialVertices().isEmpty()) {
				//initalStep dependency: generate exactly one dependency even if a subgraf has multiple initial steps
				hierarchyOrder.getDependencies().add(new HierarchyDependency(null, hypergraf, 
						subgraf, InitializationType.initialStep));
			}
			//sourceTransition dependency: generate exactly one dependency even if a subgraf has multiple source transitions
			if (!subgraf.getSourceEdges().isEmpty()) {
				hierarchyOrder.getDependencies().add(new HierarchyDependency(null, hypergraf, 
						subgraf, InitializationType.sourceTransition));
			}
			
		}
	}
	



	private void runInitialGeneration() {
		//FIXME: So wie es ist l�uft es wohl, es werden nur keine MacroExpansions unterst�tzt, die zu zwei Makroschritten geh�ren
		//daf�r m�sste hier wohl die Reihenfolge angepasst werden. Man m�sste mit den Edges starten. W�rde zu Art Worklist Algorithmus f�hren
		//Alternative w�re, dass man ME kopiert, wenn sie mehrmals referenziert werden. Wohl die einfachere Alternative
		//Subgrafs:
		for (Grafcet p : grafcet.getPartialGrafcets()) {
			if (p instanceof PartialGrafcet) {
				Subgraf subgraf = new Subgraf((PartialGrafcet) p);
				hypergraf.getSubgrafs().add(subgraf);
			}
		}
		//Macrostepexpansions:
		normalizeMacrosteps();
		//Vertices and enclosed/forced dependencies of HierarchyOrder:
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			if(subgraf.getPartialGrafcet() != null) {
				for (InitializableType step : subgraf.getPartialGrafcet().getSteps()) {
					addVertexAndCorrespondingDependency(step, subgraf);
				} 
			}
		}
		//Edges:
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			if(subgraf.getPartialGrafcet() != null) {
				for (Transition transition : subgraf.getPartialGrafcet().getTransitions()) {
					createEdge(transition, subgraf, subgraf.getPartialGrafcet());
				}
			}
		}
	}
	
	/**
	 * REQUIRE: step is step, enclosing step or exit/entry Step
	 * @param step
	 * @param subgraf
	 */
	private void addVertexAndCorrespondingDependency(Node step, Subgraf subgraf) {
		//add vertex:
		Vertex vertex = new Vertex(step);
		subgraf.getVertices().add(vertex);
		//enclosed dependencies:
		if(step instanceof EnclosingStep) {
			//for all enclosed partial Grafcets 
			for (PartialGrafcet enclosedPartialGrafcet : ((EnclosingStep)step).getPartialGrafcets()) {
				// add edge
				hierarchyOrder.getDependencies().add(new HierarchyDependency(vertex, subgraf, 
						hypergraf.getSubgraf(enclosedPartialGrafcet), InitializationType.enclosed));
			}
		}
		//forced dependency:
		//TODO: alternative Implementierung: �ber ForcingOrders iterieren (nur wo?)
		for (ActionLink actionLink : subgraf.getPartialGrafcet().getActionLinks()) {
			if (actionLink.getStep().equals(step) && actionLink.getActionType() instanceof ForcingOrder) {
				hierarchyOrder.getDependencies().add(new HierarchyDependency(vertex, subgraf, 
						hypergraf.getSubgraf(((ForcingOrder)actionLink.getActionType()).getPartialGrafcet()), InitializationType.forced));
			}
		}
	}
	
	/**
	 * REQUIRE: no nested Macrosteps
	 */
	private void normalizeMacrosteps() {
		//collect macro steps:
		LinkedHashSet<Macrostep> macrosteps = new LinkedHashSet<Macrostep>();
		for (Grafcet grafcet : grafcet.getPartialGrafcets()) {
			for (Macrostep macrostep : grafcet.getMacrosteps()) {
				macrosteps.add(macrostep);
			}
		}
		
		//check if expansion is referenced twice:
		LinkedHashMap<MacrostepExpansion, LinkedHashSet<Macrostep>> macrostepExpansionToStepMap = new LinkedHashMap<MacrostepExpansion, LinkedHashSet<Macrostep>>();
		for (Grafcet macrostepExpansion : grafcet.getPartialGrafcets()) {
			if (macrostepExpansion instanceof MacrostepExpansion) {
				LinkedHashSet<Macrostep> doubleReferenceMacrosteps = new LinkedHashSet<Macrostep>();
				for (Macrostep macrostep : macrosteps) {
					if (macrostep.getExpansion().equals(macrostepExpansion)) {
						doubleReferenceMacrosteps.add(macrostep);
					}
				}
				if (doubleReferenceMacrosteps.size() > 1) {
					throw new IllegalArgumentException("A macro step expansion referenced by multiple macrosteps is not supported");
					
					//copy expansion:
					//eigentlich w�re die L�sung relativ simpel bei einer bestehenden Kopiermethoden.
					//allgemeines Problem: deep copy in java
					//problem von EMF: Serialization scheint nicht zu funktionieren
					//problem vom Modell: arcs uns actionsLinks sollen auch kopiert werden
							//Um Referenz zu behalten m�ssten man immer wenn man z.B. einen Schritt kopiert 
							//in allen arc.source, arc.target und actionLink.step nach dem Schritt suchen, den zur Kopie hinzuf�gen. 
							//Wie finde ich aber die richtige Kopie?
//					for (Macrostep macrostep : doubleReferenceMacrosteps) {
////						macrostep.setExpansion((MacrostepExpansion)deepClone(macrostepExpansion));
//						Copier copier = new Copier();
////						copier.copy(macrostepExpansion);
//						macrostep.setExpansion((MacrostepExpansion) copier.copy(macrostepExpansion));
//					}
				}
			}
		}
		
		
		
		//normalize:
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			if (subgraf.getPartialGrafcet() instanceof PartialGrafcet) {
				for (Macrostep macrostep : subgraf.getPartialGrafcet().getMacrosteps()) {
					//normalize MacrostepExpansion according to Schumacher
					for (InitializableType stepInME : macrostep.getExpansion().getSteps()) {
						addVertexAndCorrespondingDependency(stepInME, subgraf);		//special case when a MacrstepExpansion contains a ForcingOrder or EnclosingStep
					}
					for (EntryStep entryStep : macrostep.getExpansion().getEntryStep()) {
						subgraf.getVertices().add(new Vertex(entryStep));
					}
					subgraf.getVertices().add(new Vertex(macrostep.getExpansion().getExitStep()));
					for (Transition transition : macrostep.getExpansion().getTransitions()) {
						createEdge(transition, subgraf, macrostep.getExpansion());
					}
				}
			}
		}
	}
	
	private static <T> T deepClone(T object){
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		    objectOutputStream.writeObject(object);
		    ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		    ObjectInputStream objectInputStream = new ObjectInputStream(bais);
		    return (T) objectInputStream.readObject();
		}catch (Exception e) {
			e.printStackTrace();
		    return null;
		}
	}
	
	/**
	 * REQUIRE: step is InitializableType (i.e. Step or EnclosingStep) or Macrostep
	 * @param collToAddStep result of algorithm will be added to this collection (eitzer InitializableType oder Enty or Exitstep of corresponding MacrostepExpansion, iff step is a Macrostep
	 * @param step either a InitializableType or a Macrostep
	 * @param subgrafOfStep corresponding subgraf to partial Grafcet containing step
	 * @param isDownstream falg; if true step is a downstream Step of the corresponding Transition
	 * @return true, step is a Macrostep
	 */
	private boolean handleMacrostep (Collection<Node> collToAddStep, Node step, boolean isDownstream) {
		if (step instanceof Macrostep) {			
			//Hier nur enty oder exit step hinzuf�gen
			if (isDownstream) {
				//add entrysteps of MacrostepExpanstion
				for (EntryStep entryStep : ((Macrostep)step).getExpansion().getEntryStep()) {
					collToAddStep.add(entryStep);
				}
			} else {
				//add exitStep of MacrostepExpanstion
				collToAddStep.add(((Macrostep)step).getExpansion().getExitStep());
			}
			return true;
		} else if (step instanceof Step ||
				step instanceof EnclosingStep ||
				step instanceof EntryStep ||
				step instanceof ExitStep) {
			collToAddStep.add(step);
		} else {
			throw new IllegalArgumentException("Error in algortihm or model");
		}
		return false;
	}
	
	/**
	 * REQUIRE: all vertices exist in subgraf already
	 * REQUIRE: grafcet containing transition
	 * @param transition
	 * @param subgraf
	 * @param grafcet patialGrafcet or macrostepExpansion containing the transition
	 */
	
	private void createEdge(Transition transition, Subgraf subgraf, Grafcet grafcet) {

		if (!grafcet.getTransitions().contains(transition)) {
			throw new IllegalArgumentException("grafcet must contain transition");
		}
		if (grafcet instanceof PartialGrafcet) {
			if (!subgraf.getPartialGrafcet().equals(grafcet)) {
				throw new IllegalArgumentException("grafcet must correspond to subgraf");
			}
		} 
		
		
		Edge edge = new Edge(transition);
		LinkedHashSet<Node> upstreamSteps = new LinkedHashSet<Node>();
		LinkedHashSet<Node> downstreamSteps = new LinkedHashSet<Node>();
		for (Arc arc : grafcet.getArcs()) {
			if ( arc.getTarget().equals(transition)) {
				//sources:
				if (arc.getSource() instanceof Synchronization){
					for(Arc upstreamOfArc : grafcet.getArcs()){
						if(upstreamOfArc.getTarget() == arc.getSource()){
							handleMacrostep(upstreamSteps, upstreamOfArc.getSource(), false);
						}
					}
				}
				else{
					handleMacrostep(upstreamSteps, arc.getSource(), false);
				}
			}
			if ( arc.getSource().equals(transition)) {
				//targets:
				if (arc.getTarget() instanceof Synchronization){
					for(Arc downstreamOfArc : grafcet.getArcs()){
						if(downstreamOfArc.getSource() == arc.getTarget()){
							handleMacrostep(downstreamSteps, downstreamOfArc.getTarget(), true);
						}
					}
				}
				else{
					handleMacrostep(downstreamSteps, arc.getTarget(), true);
				}
			}
		}
		//get vertices from steps and add to edge
		for (Vertex v : subgraf.getVertices()) {
			for (Node node : upstreamSteps) {
				//ich brauche existierende Vertex mit Schritt node
				if (v.getStep().equals(node)) {
					edge.getUpstream().add(v);
				}
			}
			for (Node node : downstreamSteps) {
				//ich brauche existierende Vertex mit Schritt node
				if (v.getStep().equals(node)) {
					edge.getDownstream().add(v);
				}
			}
		}
		subgraf.getEdges().add(edge);
	}
}
