package de.hsu.grafcet.staticAnalysis.popup;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.resource.ResourceSet;
//import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import apron.ApronException;
import apron.Box;
import apron.Manager;
import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.SequAbstractInterpreter;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.ModAbstractInterpreter;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergrafTransromation.HypergrafcetGenerator;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.StructuralConcurrencyAnalyzer;
import de.hsu.grafcet.staticAnalysis.verification.Detecter;
import de.hsu.grafcet.staticAnalysis.verification.FlawedTransitionDetecter;
import de.hsu.grafcet.staticAnalysis.verification.HierarchicalConflictDetecter;
import de.hsu.grafcet.staticAnalysis.verification.RaceConditionDetecter;
import de.hsu.grafcet.staticAnalysis.verification.TransientRunDetecter;

public class RunAbstractInterpretation implements IObjectActionDelegate{

	private Shell shell;
	protected List<IFile> files;	
	private Grafcet selectedGrafcet;
	private int analysis;
	private String message = "Checking was executed.";
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction arg0) {
		if (files != null) {

			//TEST MessageDialog:
			//			try {
//				analysis = MessageDialog.open(MessageDialog.QUESTION, shell, 
//						"Choose properties to analyze.", 
//						"Message", 0, "", "2", "3", "4", "5", "6");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}

			
			IRunnableWithProgress operation = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						Iterator<IFile> filesIt = files.iterator();
						while (filesIt.hasNext()) {
													
							IFile model = (IFile)filesIt.next();
							URI modelURI = URI.createPlatformResourceURI(model.getFullPath().toString(), true);
							IContainer target = model.getProject().getFolder("Static_Analysis");
							try {
								//System.out.println(analysis);
								ResourceSet resSet = new ResourceSetImpl();
								Resource res = resSet.getResource(modelURI,	true);
								
								
								HierarchyOrder hierarchyOrder = new HierarchyOrder((Grafcet)res.getContents().get(0));
                                String out1  = hierarchyOrder.structuralAnalysis();
                                Util.createOutputFile(out1 , target.getLocation().toString() + "/Result_Concurrency_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
                                hierarchyOrder.runAbstractInterpretation();
                                
//                                String out12 = hierarchyOrder.runConcurrentAbstractInterpretation();
//                                Util.createOutputFile(out12 , target.getLocation().toString() + "/Result_Concurrency_AI_TEST_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
                                
                                String out2 = hierarchyOrder.getAIResult();
                                Util.createOutputFile(out2, target.getLocation().toString() + "/Result_AI_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
                                String out3 = hierarchyOrder.getAIFullLog();
                                Util.createOutputFile(out3, target.getLocation().toString() + "/Result_AI_fullLog_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
                                
                                Detecter transitions = new FlawedTransitionDetecter(hierarchyOrder);
                                Detecter hierConflicts = new HierarchicalConflictDetecter(hierarchyOrder);
                                Detecter raceConditions = new RaceConditionDetecter(hierarchyOrder);
                                Detecter transientRuns = new TransientRunDetecter(hierarchyOrder);

//                                transitions.runAnalysis();	//TODO: Timeouted
//                                hierConflicts.runAnalysis();
//                                raceConditions.runAnalysis();
//                                transientRuns.runAnalysis();
////
//                                String out4 = transitions.getResults();
//                                out4 += hierConflicts.getResults();
//                                out4 += raceConditions.getResults();
//                                out4 += transientRuns.getResults();                                                                
//                                Util.createOutputFile(out4, target.getLocation().toString() + "/Result_AI_veri_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt"); 
								
							} catch (ApronException e){
								//TODO unschöne Implementierung, mit dem Error, da es nur in der einen Date ist. Müsste in allen anderen auch sein --> Methode auslagern
//								Util.createOutputFile("Error: " + e, target.getLocation().toString() + "/Result_AI_veri_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
								message = "ERROR: Please inspect console log for details.";
								e.printStackTrace();
							} catch (Exception e) {
//								Util.createOutputFile("Error: " + e, target.getLocation().toString() + "/Result_AI_veri_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
								message = "ERROR: Please inspect console log for details.";
								e.printStackTrace();
							}finally {
//								Util.createOutputFile("Error", target.getLocation().toString() + "/Result_AI_veri_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
								model.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
							}
						}
					} catch (CoreException e) {
						IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
						Activator.getDefault().getLog().log(status);
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, operation);
			} catch (InvocationTargetException e) {
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				Activator.getDefault().getLog().log(status);
			} catch (InterruptedException e) {
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				Activator.getDefault().getLog().log(status);
			}
		}
		MessageDialog.openInformation(
				shell,
				"Actions",
				message);
		
	}
	
	
	

	
	

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@SuppressWarnings("unchecked")
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			files = ((IStructuredSelection) selection).toList();
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	
	
}