package de.hsu.grafcet.staticAnalysis.hypergraf;

import java.util.LinkedHashSet;
import java.util.Set;

import de.hsu.grafcet.*;

public class Vertex implements Statement{

	Node step; //sollte nur EntyStep, ExitStep oder InitializableType sein
	int id;
	Set<ActionType> actionTypes = new LinkedHashSet<ActionType>();
	private int visited = 0;
	
	public Vertex(Node step) {
		super();
		this.setStep(step);
		this.id = step.getId();
	}
	
	public Node getStep() {
		return step;
	}
	
	/**
	 * REQUIRE: step is Step, EclosingStep, EntryStep or ExitStep
	 * @param step
	 */
	public void setStep(Node step) {
		if (step instanceof Step ||
				step instanceof EnclosingStep ||
				step instanceof EntryStep ||
				step instanceof ExitStep) {
			this.step = step;
		} else {
			throw new IllegalArgumentException("Step must be of type Step, EclosingStep, EntryStep or ExitStep.");
		}	
	}
	
	public Set<ActionType> getActionTypes() {
		//FIXME transformation muss diese hinzufügen
		//FIXME bedingte Aktionen müssen normalisiert werden
			//Es werden nur Aktionen bei Deaktivierung Normalisiert: 
			//Aktion wird als "Aktion bei Aktivierung" an nachfolgenden Schritt angehängt. ggf. neue Klass sinnvoll?
		//throw new NotImplementedException();
		return actionTypes;
	}
	public void addActionType(ActionType actionType) {
		actionTypes.add(actionType);
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
		return Integer.toString(id);
	}
	
	
	
}
