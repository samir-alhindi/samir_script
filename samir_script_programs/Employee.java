
public class Employee {
	private String name;
	private int salary;

	Employee(String name, int salary){
		this.name = name;
		this.salary = salary;
	}

	public void set_name(String name){
		this.name = name;
	}
	public void set_salary(int salary){
		this.salary = salary;
	}

	public String get_name(){
		return name;
	}
	public int get_salary(){
		return salary;
	}

}