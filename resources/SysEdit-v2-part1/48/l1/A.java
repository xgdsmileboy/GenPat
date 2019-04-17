
public class A {

	boolean flag = false;
	boolean flag2 = false;
	
	B fContainer = null;
	Object fComposite = null;
	int v1 = 1;
	int v2 = 2;
	
	float f1 = 1;
	float f2 = 2;
	
	double d1 = 1;
	double d2 = 2;
	
	public void flush(IProgressMonitor monitor){
		saveContent(getInput());
	}
	
	private int getInput(){
		return 0;
	}
	
	public void run(){
		saveContent(getInput());
	}
	
	private void saveContent(int i){
		
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
	
	//examples for SP
	public void m1(){
		if(isLeftDirty() && !isRightDirty()){
			System.out.println("Left");
		}
	}
	public void m2(){
		if(!isLeftDirty() && isRightDirty()){
			System.out.println("Right");
		}
	}
	public void m3(){
		if(!isLeftDirty() && !isRightDirty()){
			System.out.println("None");
		}
	}
	public void m4(){
		if(isLeftDirty() && isRightDirty()){
			System.out.println("Both");
		}
	}
	
	public void m5(){
		if(isUpDirty() && !isDownDirty()){
			System.out.println("Boy");
		}
	}
	
	
	//examples for NCP
	public Boolean m6(){
		if(fContainer == null){
			return C.findActionBars();
		}
		return fContainer.isLeftDirty();	
	}
	
	//examples for NCP
	public Boolean m7(){//1
		if(fContainer == null){
			return C.findServiceLocator();
		}
		return fContainer.getServiceLocator();	
	}
	
	public Boolean m8(){//2
		if(fContainer == null){
			return C.findServiceLocator2();
		}
		return fContainer.getServiceLocator2();
	}

	public Boolean m9(){//3
		if(fContainer == null){
			return C.findServiceLocator3();
		}
		return fContainer.getServiceLocator3();
	}
	
	public Boolean m10(){
	  if (fContainer == null) {
	    return C.findServiceLocator4();
	  }
	  return fContainer.getServiceLocator4();
	}

	public Boolean m11(){//5
		if(fContainer == null){
			return C.findServiceLocator5();
		}
		return fContainer.getServiceLocator5();
	}
}
