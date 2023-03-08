package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.*;

public class RaceConditionDetecter {

	Hypergraf hyperGrafcet;
	Set<Set<InitializableType>> setsOfConcurrentSteps; //assumed to be reachable //including Enclosing Steps
	Set<Set<StepActionObject>> setsOfConcurrentActions;
	Set<List<StepActionObject>> setsOfConcurrentPairs;
	Set<List<StepActionObject>> setsOfReceConditionPairs;
	

	/**
	 * 
	 * @param hypergraf to be checked
	 * @param setsOfConcurrentSteps: concrrent steps of hypergraf. Assumed to be reachable and indluding eclosing steps
	 */
	
	public RaceConditionDetecter(Hypergraf hypergraf, Set<Set<InitializableType>> setsOfConcurrentSteps) {
		this.hyperGrafcet = hypergraf;
		this.setsOfConcurrentSteps = setsOfConcurrentSteps;
	}
	
	public String runAnalysis() {
		identifyConcurrentActions();
		checkConcurrencyOfAssociatedSteps();
		checkTriggerConditions();
		if (setsOfReceConditionPairs.isEmpty()) {
			return "No race conditions detected";
		} else {
			String out = "The following race conditions were detected: \n";
			for (List<StepActionObject> pair : setsOfReceConditionPairs) {
				out += "Action " + pair.get(0).getAction() + " is in conflict with " + pair.get(1).getAction() + "\n";
			}
			return out;
		}
	}
	
	private boolean isOverlapping(Term t1, Term t2) {
		//FIXME
	//APRON oder SMT?
//		So eine Funktion auch für andere Stelle
		return false;
	}
	
	private boolean isRaceCondition(Set<Term> terms1, Set<Term> terms2) {
		for (Term t1 : terms1) {
			for (Term t2 : terms2) {
				if (isOverlapping(t1, t2)) {
					return true; //not every possibility is checked
				}
			}
		}
		return false;
	}
	
	private void checkTriggerConditions() {
		setsOfReceConditionPairs = new HashSet<List<StepActionObject>>();
		for (List<StepActionObject> pair : setsOfConcurrentPairs) {
			if (isRaceCondition(pair.get(0).getTriggerinTerms(), pair.get(1).getTriggerinTerms())) {
				setsOfReceConditionPairs.add(pair);
			}
		}
	}
	
	private <T> Set<List<T>> getSetOfPairs(Set<T> set){
		if (set.size() == 2) {
			return Collections.singleton(new ArrayList<>(set)); // HashSet<List<T>>() {{ add( new ArrayList(set));}};
		} else if (set.size() < 2) {
			throw new IllegalArgumentException("set size is less than 2");
		}
		Set<List<T>> setOfPairs = new HashSet<List<T>>();
		Set<T> visited = new HashSet<T>();
		for (T t1 : set) {
			Set<T> remaining = new HashSet<T>(set);
			visited.add(t1);
			remaining.removeAll(visited);
			for (T t2 : remaining) {
				List<T> pair = new ArrayList<T>();
				pair.add(t1);
				pair.add(t2);
				setOfPairs.add(pair);
			}
		}
		return setOfPairs;
	}
	
	private void checkConcurrencyOfAssociatedSteps() {
		setsOfConcurrentPairs = new HashSet<List<StepActionObject>>();
		for (Set<StepActionObject> concurrentActions : setsOfConcurrentActions) {
			for (List<StepActionObject> pair : getSetOfPairs(concurrentActions)) {
				for (Set<InitializableType> concurrentSteps : setsOfConcurrentSteps) {
					if (concurrentSteps.contains(pair.get(0).getStep()) && concurrentSteps.contains(pair.get(1).getStep()) ) {
						setsOfConcurrentPairs.add(pair);
					}
				}
				
			}
		}
	}
	
	private void identifyConcurrentActions() {
		List<StepActionObject> storedActions = new ArrayList<StepActionObject>();
		for (Subgraf pg : hyperGrafcet.getSubgrafs()) {
			for (Vertex vertex : pg.getVertices()) {
				for (ActionType action : vertex.getActionTypes()) {
					if (action instanceof StoredAction) {
						storedActions.add(new StepActionObject(vertex, (StoredAction)action));
					}
				}
			}
			
		}
		setsOfConcurrentActions = new HashSet<Set<StepActionObject>>();
		while (!storedActions.isEmpty()) {
			Set<StepActionObject> concurrentActions = new HashSet<StepActionObject>();
			VariableDeclaration varDecl = storedActions.get(0).getAction().getVariable().getVariableDeclaration();
			for (StepActionObject sa2 : storedActions) {
				if (sa2.getAction().getVariable().getVariableDeclaration().equals(varDecl)) {
					concurrentActions.add(sa2);
				}
			}
			storedActions.removeAll(concurrentActions);
			if (concurrentActions.size() > 1) {
				setsOfConcurrentActions.add(concurrentActions);
			}
		}		
	}
	
	
	private class StepActionObject {
		private Step step;
		private StoredAction action;
		private Set<Term> triggerinTerms = new HashSet<Term>();
		private Vertex vertex;
		
		public StepActionObject(Vertex vertex, StoredAction action) {
			if(!vertex.getActionTypes().contains(action)) {
				throw new IllegalArgumentException("action not associated to vertex; might be an error in transformation?");
			}
			this.action = action;
			this.vertex = vertex;
			setTerms();
		}
		
//		private void setAssociatedStep(){
//			for (Subgraf pg : hyperGrafcet.getSubgrafs()) {
//				for (ActionLink actionLink : pg.getPartialGrafcet().getActionLinks()) {
//					if (actionLink.getActionType().equals(action)) {
//						this.step = actionLink.getStep();
//						return;
//					}
//				}
//			}
//		}
		
		private void setTerms() {
			switch (action.getStoredActionType()) {
			case ACTIVATION: {
				for (Edge e : hyperGrafcet.getUpstreamEdges(vertex)) {
					triggerinTerms.add(e.getTransition().getTerm());
				}
				break;
			}
			case DEACTIVATION:{
				for (Edge e : hyperGrafcet.getDownstreamEdges(vertex)) {
					triggerinTerms.add(e.getTransition().getTerm());
				}
				break;
			}
			case EVENT:{
				triggerinTerms.add(action.getTerm());
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + action.getStoredActionType());
			}
		}
		
		public StoredAction getAction() {
			return action;
		}
		public Step getStep() {
			return step;
		}
		public Set<Term> getTriggerinTerms() {
			return triggerinTerms;
		}
	}
}
