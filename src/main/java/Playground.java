import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Playground {

	public static void main(String[] args) throws Exception {

		Map<String, String> params = new HashMap<>();
		params.put("order_eqs", "123");
		params.put("status_in", "completed,processed");

		List<String> allowedParam = Arrays.asList("id", "status");

		for (String key : params.keySet()) {
			String[] parts = key.split("_");
			if (parts.length != 2 || !allowedParam.contains(parts[0]) || !isValidQueryParamOperator(parts[1])) {
				throw new Exception("Invalid Paramers");
			}
		}

	}

	public static boolean isValidQueryParamOperator(String operator) {
		return Arrays.asList("eq", "in", "neq", "gte", "gt", "lte", "lt").contains(operator);
	}
}
