import java.util.ArrayList;
import java.util.List;

public class Valeria {

	public static void main(String[] args) {
		int [] a = {-2, -3, 4, -1, -2, 1, 5, -3};
        System.out.println("Maximum contiguous sum is " +
                                       maxSubArraySum(a));

	}

	 static int maxSubArraySum(int a[])
	    {
	        int size = a.length;
	        int max_so_far = Integer.MIN_VALUE, max_ending_here = 0;
	 
	        for (int i = 0; i < size; i++)
	        {
	            max_ending_here = max_ending_here + a[i];
	            if (max_so_far < max_ending_here)
	                max_so_far = max_ending_here;
	            if (max_ending_here < 0)
	                max_ending_here = 0;
	        }
	        return max_so_far;
	    }
	public static List<Integer> findSubarrayWithSum(int[] arr, int target) {
		int start = 0;
		int currentSum = 0;
		List<Integer> subarray = new ArrayList<>();

		for (int end = 0; end < arr.length; end++) {
			// Add the current element to the current sum
			currentSum += arr[end];

			// While the current sum is greater than the target, subtract the start element
			while (currentSum > target && start <= end) {
				currentSum -= arr[start];
				start++;
			}

			// If the current sum equals the target, return the subarray
			if (currentSum == target) {
				for (int i = start; i <= end; i++) {
					subarray.add(arr[i]);
				}
				return subarray;
			}
		}

		return subarray; // Return an empty list if no subarray is found
	}
}
