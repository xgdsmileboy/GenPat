
public class B {

	public boolean isLeftDirty(){
		return false;
	}
	
	public boolean isRightDirty(){
		return true;
	}
	
	public boolean isUpDirty(){
		return false;
	}
	
	public boolean isDownDirty(){
		return true;
	}
	
	public void m1(){
		if(isDownDirty() && !isUpDirty()){
			System.out.println("Left");
		}
	}
	public void m2(){
		if(!isDownDirty() && isUpDirty()){
			System.out.println("Right");
		}
	}
	public void m3(){
		if(!isDownDirty() && !isUpDirty()){
			System.out.println("None");
		}
	}
	public void m4(){
		if(isDownDirty() && isUpDirty()){
			System.out.println("Both");
		}
	}
	public void m5(){
		if(isRightDirty() && !isLeftDirty()){
			System.out.println("Boy");
		}
	}
	
	public boolean getServiceLocator(){
		return true;
	}
	
	public boolean getServiceLocator2(){
		return false;
	}
	
	public boolean getServiceLocator3(){
		return true;
	}
	
	public boolean getServiceLocator4(){
		return false;
	}
	
	public boolean getServiceLocator5(){
		return true;
	}
}
