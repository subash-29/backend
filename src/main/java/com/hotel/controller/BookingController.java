package com.hotel.controller;

import com.hotel.dto.Dtos.*;
import com.hotel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(Authentication auth, @Valid @RequestBody BookingRequest req) {
        return bookings.create(auth.getName(), req);
    }

    @GetMapping("/my")
    public List<BookingResponse> mine(Authentication auth) { return bookings.mine(auth.getName()); }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return bookings.cancel(id, auth.getName(), admin);
    }
}
