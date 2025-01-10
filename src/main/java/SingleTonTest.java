
public class SingleTonTest {

	private static SingleTonTest instance;

	private SingleTonTest() {

	}

	public static SingleTonTest getInstance() {
		if (instance == null) { // Check if instance is null
			synchronized (SingleTonTest.class) {
				if (instance == null) {
					try {
						// Simulate some delay to increase the chance of thread collision
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					instance = new SingleTonTest(); // Create a new instance
				}
			}

		}

		return instance;
	}

}
