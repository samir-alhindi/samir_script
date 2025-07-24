
public class Course { 
	private String name;
	private int credit_hours;
	private Employee tutor;
	private String description;
	private String track;

	Course(String name, int credit_hours, Employee tutor, String description, String track){
		this.name = name;
		this.description = description;
		this.track = track;
		this.credit_hours = credit_hours;
		this.tutor = tutor;
	}

	public void set_name(String name){
		this.name = name;
	}
	public void set_description(String description){
		this.description = description;
	}
	public void set_track(String track){
		this.track = track;
	}
	public void set_credit_hours(int credit_hours){
		this.credit_hours = credit_hours;
	}
	public void set_tutor(Employee tutor){
		this.tutor = tutor;
	}

	public String get_name(){
		return name;
	}
	public String get_description(){
		return description;
	}
	public String get_track(){
		return track;
	}
	public int get_credit_hours(){
		return credit_hours;
	}
	public Employee get_tutor(){
		return tutor;
	}

}