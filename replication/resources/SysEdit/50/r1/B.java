
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
	
	//sp examples
	public void m1(){
		if(isUpDirty() && !isDownDirty()){
			System.out.println("Left");
		}
	}
	public void m2(){
		if(!isUpDirty() && isDownDirty()){
			System.out.println("Right");
		}
	}
	public void m3(){
		if(!isUpDirty() && !isDownDirty()){
			System.out.println("None");
		}
	}
	public void m4(){
		if(isUpDirty() && isDownDirty()){
			System.out.println("Both");
		}
	}
	
	public void m5(){
		if(isLeftDirty() && !isRightDirty()){
			System.out.println("Boy");
		}
	}
	
	public Boolean getServiceLocator(){
		return true;
	}
	
	public Boolean getServiceLocator2(){
		return false;
	}
	
	public Boolean getServiceLocator3(){
		return true;
	}
	
	public Boolean getServiceLocator4(){
		return false;
	}
	
	public Boolean getServiceLocator5(){
		return true;
	}
}
