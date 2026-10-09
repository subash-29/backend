package com.hotel;

import com.hotel.dto.Dtos.BookingRequest;
import com.hotel.entity.Role;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.User;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {
    @Autowired BookingService service;
    @Autowired RoomRepository rooms;
    @Autowired UserRepository users;
    @Autowired BookingRepository bookings;

    @Test
    void onlyOneOfManyConcurrentRequestsWins() throws Exception {
        Room room = rooms.save(Room.builder().roomNumber("T-1").type(RoomType.DOUBLE).capacity(2)
                .pricePerNight(new BigDecimal("100")).active(true).build());
        int n = 8;
        for (int i = 0; i < n; i++) {
            users.save(User.builder().name("u" + i).email("u" + i + "@t.com").password("x").role(Role.GUEST).build());
        }
        LocalDate in = LocalDate.now().plusDays(5), out = in.plusDays(2);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), failed = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            final String email = "u" + i + "@t.com";
            pool.submit(() -> {
                try {
                    start.await();
                    service.create(email, new BookingRequest(room.getId(), in, out, 2));
                    ok.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        assertEquals(1, ok.get(), "exactly one booking must succeed");
        assertEquals(n - 1, failed.get());
        assertEquals(1, bookings.count());
    }
}
