package com.hotel.config;

import com.hotel.entity.*;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seeds a default admin and some sample rooms on first start. CHANGE THE ADMIN PASSWORD in production. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final RoomRepository rooms;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, RoomRepository rooms, PasswordEncoder encoder) {
        this.users = users;
        this.rooms = rooms;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmail("admin@hotel.com")) {
            users.save(User.builder().name("Hotel Admin").email("admin@hotel.com")
                    .password(encoder.encode("Admin@123")).role(Role.ADMIN).build());
        }
        if (rooms.count() == 0) {
            seed("101", RoomType.SINGLE, 1, "1999", "Cozy single room with a work desk and city view.");
            seed("102", RoomType.SINGLE, 1, "2199", "Quiet single room on the garden side.");
            seed("201", RoomType.DOUBLE, 2, "3499", "Spacious double room with a king bed and balcony.");
            seed("202", RoomType.DOUBLE, 3, "3999", "Double room with an extra sofa bed for a third guest.");
            seed("301", RoomType.DELUXE, 3, "5999", "Deluxe room with a lounge area, bathtub and minibar.");
            seed("302", RoomType.DELUXE, 4, "6499", "Family deluxe room with two queen beds.");
            seed("401", RoomType.SUITE, 4, "9999", "Presidential suite with living room and panoramic views.");
        }
    }

    private void seed(String no, RoomType type, int cap, String price, String desc) {
        rooms.save(Room.builder().roomNumber(no).type(type).capacity(cap)
                .pricePerNight(new BigDecimal(price)).description(desc).active(true).build());
    }
}
