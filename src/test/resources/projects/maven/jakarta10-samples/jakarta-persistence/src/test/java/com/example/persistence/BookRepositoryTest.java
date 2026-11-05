package com.example.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BookRepositoryTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = Persistence.createEntityManagerFactory("samplePU");
    }

    @AfterAll
    static void tearDown() {
        emf.close();
    }

    @Test
    void savesAndQueriesByAuthor() {
        BookRepository repo = new BookRepository(emf);
        Book saved = repo.save(new Book("Effective Java", "Joshua Bloch"));
        repo.save(new Book("Java Puzzlers", "Joshua Bloch"));
        repo.save(new Book("Clean Code", "Robert Martin"));

        assertNotNull(saved.getId());
        List<Book> books = repo.findByAuthor("Joshua Bloch");
        assertEquals(2, books.size());
        assertEquals("Effective Java", books.get(0).getTitle());
    }
}
