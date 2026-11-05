package com.example.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class BookRepository {

    private final EntityManagerFactory emf;

    public BookRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Book save(Book book) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(book);
            em.getTransaction().commit();
            return book;
        }
    }

    public List<Book> findByAuthor(String author) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createNamedQuery("Book.findByAuthor", Book.class)
                    .setParameter("author", author)
                    .getResultList();
        }
    }
}
