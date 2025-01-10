import java.util.Arrays;
import java.util.Comparator;
import java.util.List;import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SingleTonThread {

	public static void main(String[] args) {
//		Thread thread1 = new Thread(() -> {
//            SingleTonTest singleton1 = SingleTonTest.getInstance();
//            System.out.println("Thread 1: Singleton instance hashcode: " + singleton1.hashCode());
//        });
//
//        Thread thread2 = new Thread(() -> {
//        	SingleTonTest singleton2 = SingleTonTest.getInstance();
//            System.out.println("Thread 2: Singleton instance hashcode: " + singleton2.hashCode());
//        });
//
//        // Start both threads
//        thread1.start();
//        thread2.start();
//		List<String> nameList = Arrays.asList("ram","sita", "rahul", null , null , "deepak");
        
        List<String> nameList = Arrays.asList("ram","sita", "rahul", null , null , "deepak");
        System.out.println(nameList.stream().sorted(Comparator.nullsFirst(Comparator.naturalOrder())).collect(Collectors.toList()));
        System.out.println(nameList.stream().sorted(Comparator.nullsFirst(Comparator.naturalOrder())).collect(Collectors.toList()));

//				
//		List <String> nullList = nameList.stream().filter(name -> name == null).collect(Collectors.toList());
//		List <String> notNullList = nameList.stream().filter(name -> name != null).collect(Collectors.toList());
//		
//		nullList.addAll(notNullList);
//		System.out.println(nullList);
	}

	public static String method1() {
		try {
			int i = 9 / 0;
			System.out.println(i);
		} catch (Exception e) {
			System.out.println("exception caught");
			return "from catch";
		}			
		finally {
			System.out.println("finally block executing");
		}
		System.out.println("end");
		return "from end";
	}
}
