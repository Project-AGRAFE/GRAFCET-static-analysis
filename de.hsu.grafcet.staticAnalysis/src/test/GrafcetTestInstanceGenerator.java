package test;

import de.hsu.grafcet.ActionLink;
import de.hsu.grafcet.Arc;
import de.hsu.grafcet.EnclosingStep;
import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.GrafcetFactory;
import de.hsu.grafcet.InitializableType;
import de.hsu.grafcet.PartialGrafcet;
import de.hsu.grafcet.Step;
import de.hsu.grafcet.StoredAction;
import de.hsu.grafcet.Transition;
import terms.Addition;
import terms.IntegerConstant;
import terms.Term;
import terms.TermsFactory;
import terms.Variable;
import terms.VariableDeclaration;
import terms.VariableDeclarationType;

public class GrafcetTestInstanceGenerator {

	private Grafcet grafcet;
	private GrafcetFactory facG;
	private TermsFactory facT;
	
	public GrafcetTestInstanceGenerator() {
		facG = GrafcetFactory.eINSTANCE;
		facT = TermsFactory.eINSTANCE;
		grafcet = facG.createGrafcet();
		grafcet.setVariableDeclarationContainer(facG.createVariableDeclarationContainer());
	}
	
	
	public void addPGwLoops(int loops, int loopSize) {
		PartialGrafcet p = facG.createPartialGrafcet();
		grafcet.getPartialGrafcets().add(p);
		
		for (int i = 0; i < loops; i++) {
			addLoop(loopSize, i, p);
		}
	}
	
	public void addStoredActions(int number) {
		for (int i = 1; i <= number; i++) {
			int stepnumber = i % grafcet.getPartialGrafcets().get(0).getSteps().size();
			addStoredAction(i, grafcet.getPartialGrafcets().get(0).getSteps().get(stepnumber));
		}
			
	}
	
	/**
	 * Creates a loop of 3 steps and 3 transitions (for @param steps = 1 and two additional steps (transitions) for increasing @param steps.
	 * Adds all elements of the loop to @param p
	 * Initial step has action incrementing variable @param varName
	 */
	
	public void addLoop(int steps, int id, PartialGrafcet p) {		
		id++;
		Step s1 = facG.createStep();
		s1.setInitial(true);
		s1.setId(id*100 + 1);
		p.getSteps().add(s1);
		
		Transition t1 = facG.createTransition();
		t1.setId(id*100 + 1);
		p.getTransitions().add(t1);
		
		Arc a1 = facG.createArc();
		a1.setSource(s1);
		a1.setTarget(t1);
		p.getArcs().add(a1);
		
		Transition tUpstream = t1;
		
		for (int i = 0; i < steps; i++) {
			tUpstream = generateSequence(tUpstream, p, id * 100 + (i + 1)*2);
		}
		
		Arc a2 = facG.createArc();
		a2.setSource(tUpstream);
		a2.setTarget(s1);
		p.getArcs().add(a2);
		
		
		VariableDeclaration v1 = facT.createVariableDeclaration();
		v1.setName("l" + id);
		v1.setSort(facT.createInteger());
		v1.setVariableDeclarationType(VariableDeclarationType.INTERNAL);
		grafcet.getVariableDeclarationContainer().getVariableDeclarations().add(v1);
		
		StoredAction sa1 = facG.createStoredAction();
		Variable var = facT.createVariable();
		var.setVariableDeclaration(v1);
		sa1.setVariable(var);
		Addition value = facT.createAddition();
		Variable sub1 = facT.createVariable();
		sub1.setVariableDeclaration(v1);
		IntegerConstant sub2 = facT.createIntegerConstant();
		sub2.setValue(1);
		value.getSubterm().add(sub1);
		value.getSubterm().add(sub2);
		sa1.setValue(value);
		p.getActionTypes().add(sa1);
		
		ActionLink link1 = facG.createActionLink();
		link1.setStep(s1);
		link1.setActionType(sa1);
		p.getActionLinks().add(link1);
		

//		System.out.println(CheckPartialOrder.calculateCycles(CheckPartialOrder.extractPartialOrder(grafcet)));
	}
	
	private void addStoredAction(int id, InitializableType step) {
		Step s;
		if (!(step instanceof Step))
			return;
		else
			 s = (Step) step;
		
		PartialGrafcet p;
		if (step.eContainer() instanceof PartialGrafcet) p = (PartialGrafcet)step.eContainer();
		else throw new IllegalArgumentException();
		VariableDeclaration v1 = facT.createVariableDeclaration();
		v1.setName("k" + id);
		v1.setSort(facT.createInteger());
		v1.setVariableDeclarationType(VariableDeclarationType.INTERNAL);
		grafcet.getVariableDeclarationContainer().getVariableDeclarations().add(v1);
		
		StoredAction sa1 = facG.createStoredAction();
		Variable var = facT.createVariable();
		var.setVariableDeclaration(v1);
		sa1.setVariable(var);
		Addition value = facT.createAddition();
		Variable sub1 = facT.createVariable();
		sub1.setVariableDeclaration(v1);
		IntegerConstant sub2 = facT.createIntegerConstant();
		sub2.setValue(1);
		value.getSubterm().add(sub1);
		value.getSubterm().add(sub2);
		sa1.setValue(value);
		p.getActionTypes().add(sa1);
		
		ActionLink link1 = facG.createActionLink();
		link1.setStep(s);
		link1.setActionType(sa1);
		p.getActionLinks().add(link1);
	}
	
	private Transition generateSequence(Transition upstreamTransition, PartialGrafcet containingPG, int id) {
		Step s1 = facG.createStep();
		s1.setId(id);
		containingPG.getSteps().add(s1);
		
		Transition t1 = facG.createTransition();
		t1.setId(id);
		containingPG.getTransitions().add(t1);

		Step s2 = facG.createStep();
		s2.setId(id + 1);
		containingPG.getSteps().add(s2);
		
		Transition t2 = facG.createTransition();
		t2.setId(id + 1);
		containingPG.getTransitions().add(t2);
		
		Arc a1 = facG.createArc();
		a1.setSource(upstreamTransition);
		a1.setTarget(s1);
		containingPG.getArcs().add(a1);
		
		Arc a2 = facG.createArc();
		a2.setSource(s1);
		a2.setTarget(t1);
		containingPG.getArcs().add(a2);
	
		Arc a3 = facG.createArc();
		a3.setSource(t1);
		a3.setTarget(s2);
		containingPG.getArcs().add(a3);
		
		Arc a4 = facG.createArc();
		a4.setSource(s2);
		a4.setTarget(t2);
		containingPG.getArcs().add(a4);
		
		return t2; //end of sequence	
	}
	
	public Grafcet getGrafcet() {
		return grafcet;
	}
}
