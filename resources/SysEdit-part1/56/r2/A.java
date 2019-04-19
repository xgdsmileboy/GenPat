
public class A {
	boolean flag = false;
	boolean flag2 = false;
	
	B fContainer = null;
	boolean fContainerProvided = true;
	Object fComposite = null;
	int v1 = 1;
	int v2 = 2;
	
	float f1 = 1;
	float f2 = 2;
	
	double d1 = 1;
	double d2 = 2;
	
	public void flush(IProgressMonitor monitor){
		saveContent(getInput(), null);
	}
	
	private int getInput(){
		return 0;
	}
	
	public void run(){
		saveContent(getInput(), null);
	}
	
	private void saveContent(int i, IProgressMonitor monitor){
		
	}
	
	
	public boolean isLeftDirty(){
		return false;
	}
	
	public boolean isRightDirty(){
		return true;
	}
	
	public boolean isUpDirty(){
		return true;
	}
	
	public boolean isDownDirty(){
		return true;
	}
	public void m1(){
		if(isRightDirty() && !isLeftDirty()){
			System.out.println("Left");
		}
	}
	public void m2(){
		if(!isRightDirty() && isLeftDirty()){
			System.out.println("Right");
		}
	}
	public void m3(){
		if(!isRightDirty() && !isLeftDirty()){
			System.out.println("None");
		}
	}
	public void m4(){
		if(isRightDirty() && isLeftDirty()){
			System.out.println("Both");
		}
	}
	
	public void m5(){
		if(isDownDirty() && !isUpDirty()){
			System.out.println("Boy");
		}
	}
	
	public Boolean m6(){
		boolean actionBars = fContainer.isLeftDirty();
		if(actionBars == false && !fContainerProvided){
			return C.findActionBars();
		}
		return actionBars;
	}
	
	public Boolean m7(){
		boolean serviceLocator = fContainer.getServiceLocator();
		if(serviceLocator == false && !fContainerProvided){
			return C.findServiceLocator();
		}
		return serviceLocator;
	}
	
	public Boolean m8(){//2
		boolean serviceLocator2 = fContainer.getServiceLocator2();
		if(serviceLocator2 == false && !fContainerProvided){
			return C.findServiceLocator2();
		}
		return serviceLocator2;
	}

	public Boolean m9(){//3
		boolean serviceLocator3 = fContainer.getServiceLocator3();
		if(serviceLocator3 == false && !fContainerProvided){
			return C.findServiceLocator3();
		}
		return serviceLocator3;
	}
	
	public Boolean m10(){//4
		boolean serviceLocator4 = fContainer.getServiceLocator4();
		if(serviceLocator4 == false && !fContainerProvided){
			return C.findServiceLocator4();
		}
		return serviceLocator4;
	}
	
	public boolean m11(){//5
		boolean serviceLocator5 = fContainer.getServiceLocator5();
		if(serviceLocator5 == false && !fContainerProvided){
			return C.findServiceLocator5();
		}
		return serviceLocator5;
	}
}
