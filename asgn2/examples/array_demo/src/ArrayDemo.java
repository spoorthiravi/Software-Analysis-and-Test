public class ArrayDemo {
	public static void main(String[] args) {
		int[] anArray = new int[10];

		// assign a value to each array element 
		for (int i = 0; i < anArray.length; i++) {
			anArray[i] = i;
		}

		// print a value from each array element
		for (int i = 0; i < anArray.length; i++) {
			System.out.print(anArray[i] + " ");
		}
		System.out.println();
	}
}
