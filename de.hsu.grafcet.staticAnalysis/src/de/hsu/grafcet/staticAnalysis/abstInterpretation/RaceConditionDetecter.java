package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Manager;
import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.InitializationType;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.*;

public class RaceConditionDetecter extends Detecter{

	Hypergraf hyperGrafcet;
	Set<Set<StepActionObject>> setsOfConcurrentActions = new HashSet<Set<StepActionObject>>();; //TODO maps würden sich auch eignen mit key: varName
	Set<List<StepActionObject>> setsOfConcurrentPairs = new HashSet<List<StepActionObject>>();
	Set<List<StepActionObject>> setsOfRaceConditionPairs = new HashSet<List<StepActionObject>>();
	

	/**
	 * 
	 * @param hypergraf to be checked
	 * @param setsOfConcurrentSteps: concrrent steps of hypergraf. Assumed to be reachable and indluding eclosing steps
	 */
	
	public RaceConditionDetecter(HierarchyOrder hierarchyOrder) {
		super(hierarchyOrder);
		this.hyperGrafcet = hierarchyOrder.getHypergraf();
	}
	
	@Override
	public void runAnalysis() throws ApronException {
		identifyConcurrentActions();
		checkConcurrencyOfAssociatedSteps();
		checkTriggerConditions();	
	}

	@Override
	public String getResults() {
		if (setsOfRaceConditionPairs.isEmpty()) {
			return "\n\n== Results RaceConditionDetecter: ==\nNo race conditions detected";
		} else {
			String out = "\n\n== Results RaceConditionDetecter: ==\nThe following race conditions were detected: \n";
			for (List<StepActionObject> pair : setsOfRaceConditionPairs) {
				out += "Action " + pair.get(0).getAction() + " is in conflict with " + pair.get(1).getAction() + "\n";
			}
			return out;
		}
	}
	
	

	
	private boolean isOverlapping(Term t1, Abstract1 abs1, Term t2, Abstract1 abs2) throws ApronException {
		Manager man = new Box();
		Environment env = hierarchyOrder.getApronEnvironment();
		TransferFunction transfer1 = new TransferFunction(null, abs1, man, env);
		TransferFunction transfer2 = new TransferFunction(null, abs2, man, env);
		Abstract1 e1 = transfer1.transferTerm(t1);
		Abstract1 e2 = transfer2.transferTerm(t2);		
		Abstract1 overlap = e1.meetCopy(man, e2);
		if (overlap.isBottom(man)) {
			return false;
		} else {
			return true;
		}
	}
	
	private boolean checkTermForTrue(Term t) {
		for (Iterator<EObject> terms = t.eAllContents(); 
				terms.hasNext();) {
			EObject term = terms.next();
			if (term instanceof BooleanConstant) {
				if(((BooleanConstant)term).isValue()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isRaceCondition(StepActionObject sa1, StepActionObject sa2) throws ApronException {
		for (Statement s1 : sa1.getStatementTermMap().keySet()) {
			for (Statement s2 : sa2.getStatementTermMap().keySet()) {
				if (isOverlapping(sa1.getStatementTermMap().get(s1),hierarchyOrder.getAbstract1FromStatement(s1),
						sa2.getStatementTermMap().get(s2),hierarchyOrder.getAbstract1FromStatement(s2))) {
					return true; //not every possibility is checked
				}
			}
		}
		return false;
	}
	
	//check if triggering condition can be executed at the same time:
	private void checkTriggerConditions() throws ApronException{
		for (List<StepActionObject> pair : setsOfConcurrentPairs) {
			if (isRaceCondition(pair.get(0), pair.get(1))) {
				setsOfRaceConditionPairs.add(pair);
			}
		}
	}
	
	
	//check if associated steps can be (structurally) concurrent:
	private void checkConcurrencyOfAssociatedSteps() {
		setsOfConcurrentPairs = new HashSet<List<StepActionObject>>();
		for (Set<StepActionObject> concurrentActions : setsOfConcurrentActions) {
			for (List<StepActionObject> pair : Util.getSetOfPairs(concurrentActions)) {		
				//check if steps concurrent:
				if (hierarchyOrder.getConcurrentSteps(pair.get(0).getVertex()).contains(pair.get(1).getVertex())){ 
					setsOfConcurrentPairs.add(pair);
				}	
				//check if steps are the same:
				if (pair.get(0).getVertex().equals(pair.get(1).getVertex())) {
					setsOfRaceConditionPairs.add(pair);
				}

			}
		}
	}
	

	
	private void identifyConcurrentActions() {
		//collect actions:
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
		//collect actions that writhe the same variable:
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
		private StoredAction action;
		//private Set<Edge> triggeringEdges = new HashSet<Edge>();
		//collects all terms that could trigger this Action
		//Remember: activations by deactivation are already normalized
		private Map<Statement, Term> statementTermMap = new HashMap<Statement, Term>();
		private Vertex vertex;
		
		public StepActionObject(Vertex vertex, StoredAction action) {
			if(!vertex.getActionTypes().contains(action)) {
				throw new IllegalArgumentException("action not associated to vertex; might be an error in transformation?");
			}
			this.action = action;
			this.vertex = vertex;
			setTerms();
		}
		

		private void setTerms() {
			switch (action.getStoredActionType()) {
			case ACTIVATION: {
				for (Edge e : hyperGrafcet.getUpstreamEdges(vertex)) {
					statementTermMap.put(e, e.getTransition().getTerm());
				}
				break;
			}
			//Remember: activations by deactivation are already normalized
			//treat like activation
			case DEACTIVATION:{
				for (Edge e : hyperGrafcet.getUpstreamEdges(vertex)) {
					statementTermMap.put(e, e.getTransition().getTerm());
				}
				break;
			}
			case EVENT:{
				statementTermMap.put(vertex, action.getTerm());
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + action.getStoredActionType());
			}
		}
		
		public StoredAction getAction() {
			return action;
		}
		public Vertex getVertex() {
			return vertex;
		}
		public Map<Statement, Term> getStatementTermMap(){
			return statementTermMap;
		}
	}
}
