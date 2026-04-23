package com.validation;

import com.validation.Student;
import com.validation.validator.Validator;
import com.validation.exception.ValidationException;

public class Main {
    public static void main(String[] args) {

        System.out.println("Rozpoczynam testy walidatora...\n");

        // 1. Test studenta z DOBRYMI danymi
        System.out.println("--- Test 1: Poprawne dane ---");
        Student dobryStudent = new Student("Jan", "Kowalski", "12345678", "jan.kowalski@uczelnia.pl");

        try {
            Validator.validate(dobryStudent);
            System.out.println("Sukces: Student poprawny, brak błędów!");
        } catch (ValidationException e) {
            System.out.println("Błąd (nie powinno go tu być!):\n" + e.getMessage());
        }


        System.out.println("\n--- Test 2: Błędne dane ---");
        // 2. Test studenta z BŁĘDNYMI danymi (np. puste imię, zły email, za krótki indeks)
        Student zlyStudent = new Student("", null, "to-nie-jest-email", "12");

        try {
            Validator.validate(zlyStudent);
            System.out.println("Sukces (źle, walidator przepuścił błędne dane!)");
        } catch (ValidationException e) {
            // Oczekiwany rezultat - wypisujemy listę błędów złapanych przez Validator
            System.out.println("Złapano oczekiwane błędy walidacji:\n" + e.getMessage());
        }
    }
}