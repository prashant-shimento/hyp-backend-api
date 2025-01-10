import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.checkerframework.checker.units.qual.s;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class Majestic {

	@Getter
	@Setter
	@AllArgsConstructor
	public static class Employee {
		String name;
		int id;
		double salary;
		
	}
	
	public Map<String, List<Employee>> sortMap(Map<String, List<Employee>> employees) {
		
		Map<String, List<Employee>> sortedBySalary = employees.entrySet()
	            .stream()
	            .collect(Collectors.toMap(
	                Map.Entry::getKey,
	                entry -> entry.getValue()
	                              .stream()
	                              .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
	                              .collect(Collectors.toList())
	            ));

	        // Print the result
	        sortedBySalary.forEach((department, sortedEmployees) -> {
	            System.out.println("Department: " + department);
	            sortedEmployees.forEach(employee -> System.out.println(employee.getName()));
	        });
		return null;
	}
	public static void main(String[] args) {
		
//		Given an Employee class with attributes (id, name, salary, gender, departmentName) and a map (Map<departmentName, List<Employee>>)
//		mapping department names to lists of employees,
//		You need to sort the employee list within each department by salary in descending order (highest salary first)?
	
		
//		String name = "hi LogaNathan How are you";
//		char [] chars = name.toLowerCase().toCharArray();
//		Set<Character> a = name.toLowerCase().chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
//		a.forEach(c -> System.out.println(c));
		List<Employee> employees = Arrays.asList(
	            new Employee("Alice", 1, 50000),
	            new Employee("Bob", 2, 60000),
	            new Employee("Charlie", 3, 40000),
	            new Employee("David", 4, 70000),
	            new Employee("Eve", 5, 80000),
	            new Employee("Frank", 6, 30000)
	        );

//		List<Employee> employees  = eemployees.stream().sorted(Comparator.comparingDouble(Employee::getSalary)).collect(Collectors.toList());
		
//        Map<String, List<Employee>> departmentMap = new HashMap<>();
//        departmentMap.put("Engineering", Arrays.asList(employees.get(0), employees.get(1), employees.get(2)));
//        departmentMap.put("HR", Arrays.asList(employees.get(3), employees.get(4), employees.get(5)));
//
//        new Majestic().sortMap(departmentMap);
	}
}
