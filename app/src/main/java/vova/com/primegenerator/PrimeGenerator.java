package vova.com.primegenerator;

import java.util.ArrayList;

public class PrimeGenerator {

    public ArrayList<Integer> generate(int s1, int s2) {
        ArrayList<Integer> primes = new ArrayList<>();

        // loop through the numbers one by one
        for (int i = s1; i < s2; i++) {
            boolean isPrimeNumber = true;

            // check to see if the number is prime
            for (int j = 2; j < i; j++) {
                if (i % j == 0) {
                    isPrimeNumber = false;
                    break; // exit the inner for loop
                }
            }

            // print the number if prime
            if (isPrimeNumber) {
                primes.add(i);
            }
        }
        return primes;
    }

}