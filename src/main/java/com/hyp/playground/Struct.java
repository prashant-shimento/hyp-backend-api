package com.hyp.playground;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Struct {

    @AllArgsConstructor
    @Getter
    public static class Person {
        String name;
        int age;
    }

    public static void main(String[] args) {

        //		List<String> strings = Arrays
        //	              .asList("apple", "banana", "cherry", "date", "grapefruit");
        //		System.out.println(strings.stream().max(Comparator.comparingInt(String::length)));

        //		List<Person> persons = Arrays.asList(
        //			    new Person("Alice", 25),
        //			    new Person("Bob", 30),
        //			    new Person("Charlie", 35)
        //			);
        //
        //		System.out.println(persons.stream().mapToInt(Person::getAge).average());

        //		 List<Integer> myList = Arrays.asList(10,15,8,49,25,98,32);
        //		 System.out.println(myList.stream().map(s -> s + "").filter(s -> s.startsWith("1")).toList());
        //		System.out.println(myList.stream().map(String::valueOf).filter(s -> s.startsWith("1")).toList());

        //		  List<Integer> myList = Arrays.asList(10,15,8,49,25,98,98,32,15);
        //		  HashSet<Integer> mySet = new HashSet<>();
        //		  System.out.println(myList.stream().filter(i -> !mySet.add(i)).toList());

        //		List<Integer> list= Arrays.asList(2, 1, 8, 5, 9, 3, 4, 6, 7, 10);
        //		System.out.println(list.stream().sorted((a, b) -> {
        //			if (a % 2 == 0 && b % 2 != 0) {
        //                return -1; // a is even, b is odd
        //            } else if (a % 2 != 0 && b % 2 == 0) {
        //                return 1;  // a is odd, b is even
        //            } else {
        //                return 0;  // no need to swap
        //            }
        //		}).toList());;
        //
        //		System.out.println(Stream.concat(list.stream().filter(s -> s%2==0), list.stream().filter(s -> s%2
        // !=0)).toList());

        List<Integer> numbers = Arrays.asList(2, 4, 6, 8, 10, 11, 12, 13, 14, 15);
        System.out.println(numbers.stream().filter(Struct::isPrime).toList());
    }

    public static boolean isPrime(int number) {
        if (number <= 1) {
            return true;
        }
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}
