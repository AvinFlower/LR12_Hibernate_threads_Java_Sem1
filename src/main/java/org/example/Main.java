package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.LockModeType;
import java.util.concurrent.CountDownLatch;
import static java.lang.Thread.sleep;

public class Main {
//    static int count = 0;
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        SessionFactory factory = new Configuration()
                .addAnnotatedClass(Items.class)
                .configure("hibernate_configuration.xml")
                .buildSessionFactory();
        Session session = factory.getCurrentSession();

        try {
            session.beginTransaction();
            for (int i = 0; i < 40; i++) {
                Items items = new Items();
                session.save(items);
            }
            session.getTransaction().commit();

            CountDownLatch latch = new CountDownLatch(8);
            for (int i = 0; i < 8; i++) {
                new Thread(() -> Threading_PESSIMISTIC(factory, latch)).start();
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Session sessionForQuery = factory.getCurrentSession();
            sessionForQuery.beginTransaction();
            Object obj = sessionForQuery.createNativeQuery("SELECT SUM(val) FROM Items;").getSingleResult();
            System.out.println("SUM: "+obj);
            sessionForQuery.getTransaction().commit();

        }
        finally {
            factory.close();
        }
        long endTime = System.currentTimeMillis();
//        System.out.println("Sum of all versions: " + count);
        System.out.println("Execution time: " + (endTime - startTime) / 1000.0 + " seconds");
    }

//    private static void Threading_OPTIMISTIC(SessionFactory factory, CountDownLatch latch) {
//
//        System.out.println("Thread started: " + Thread.currentThread().getName());
//
//        for (int i = 0; i < 20000; i++) {
//            boolean success = false;
//            while (!success) {
//                Session session = factory.getCurrentSession();
//                Long rnd = ((long)(Math.random() * 40) + 1);
//                try {
//                    session.beginTransaction();
//                    CountryCity countryCity = session.get(CountryCity.class, rnd);
//                    countryCity.setVersion(countryCity.getVersion() + 1);
//                    //sleep(5);
//                    session.save(countryCity);
//                    session.getTransaction().commit();
//                    success = true;
//                } catch (OptimisticLockException e) {
//                    session.getTransaction().rollback();
//                } catch (Exception e) {
//                    session.getTransaction().rollback();
//                }
//                session.close();
//            }
////            synchronized (Main.class) {
////                count++;
////            }
//        }
//        System.out.println("Thread completed: " + Thread.currentThread().getName());
//        latch.countDown();
//    }

    private static void Threading_PESSIMISTIC(SessionFactory factory, CountDownLatch latch) {
        System.out.println("Thread started: " + Thread.currentThread().getName());

        for (int i = 0; i < 20000; i++) {
            Session session = factory.getCurrentSession();
            Long rnd = ((long)(Math.random() * 40) + 1);

            try {
                session.beginTransaction();
                Items items = session.find(Items.class, rnd, LockModeType.PESSIMISTIC_WRITE);
                items.setVal(items.getVal() + 1);
                sleep(5);
                session.save(items);
                session.getTransaction().commit();
            } catch (Exception e) {
                session.getTransaction().rollback();
            } finally {
                session.close();
            }
//            synchronized (Main.class) {
//                count++;
//            }
        }
        System.out.println("Thread completed: " + Thread.currentThread().getName());
        latch.countDown();
    }
}
