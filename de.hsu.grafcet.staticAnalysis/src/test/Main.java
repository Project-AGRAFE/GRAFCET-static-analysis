package test;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class Main {

	public static void main(String[] args) {
		
		Display display = new Display();
		Shell shell = new Shell(display);
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
		dialog.setElements(new String[] {"Linux", "Windows", "Mac"});
		dialog.setTitle("Choose");
		System.out.println(dialog.open());
		
		Object[] result = dialog.getResult();
		System.out.println(result);

	}

}
