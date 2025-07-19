
public class Student {
	private String name;
	private double gpa;
	private int total_credits;
	private int age;
	private Course[] finished_courses;

	Student(String name, double gpa, int total_credits, int age, Course[] finished_courses){
		this.finished_courses = finished_courses;
		this.total_credits = total_credits;
		this.name = name;
		this.gpa = gpa;
		this.age = age;
	}

	public void set_finished_courses(Course[] finished_courses){
		this.finished_courses = finished_courses;
	}
	public void set_total_credits(int total_credits){
		this.total_credits = total_credits;
	}
	public void set_name(String name){
		this.name = name;
	}
	public void set_gpa(double gpa){
		this.gpa = gpa;
	}
	public void set_age(int age){
		this.age = age;
	}

	public Course[] get_finished_courses(){
		return finished_courses;
	}
	public int get_total_credits(){
		return total_credits;
	}
	public String get_name(){
		return name;
	}
	public double get_gpa(){
		return gpa;
	}
	public int get_age(){
		return age;
	}

}