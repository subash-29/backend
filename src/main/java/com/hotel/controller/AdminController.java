package com.hotel.controller;

import com.hotel.dto.Dtos.*;
import com.hotel.service.BookingService;
import com.hotel.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** All routes under /api/admin/** require ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final RoomService rooms;
    private final BookingService bookings;

    public AdminController(RoomService rooms, BookingService bookings) {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    // ----- dashboard -----
    @GetMapping("/stats")
    public StatsResponse stats() { return bookings.stats(); }

    @GetMapping("/occupancy")
    public List<RoomOccupancy> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookings.occupancy(from, to);
    }

    // ----- rooms -----
    @GetMapping("/rooms")
    public List<RoomResponse> rooms() { return rooms.all(); }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest req) { return rooms.create(req); }

    @PutMapping("/rooms/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest req) { return rooms.update(id, req); }

    /** Quick activate / deactivate switch. */
    @PutMapping("/rooms/{id}/active")
    public RoomResponse setActive(@PathVariable Long id, @RequestParam boolean value) { return rooms.setActive(id, value); }

    /** Soft delete (keeps booking history). */
    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) { rooms.deactivate(id); }

    /** Hard delete - only when the room has no bookings. */
    @DeleteMapping("/rooms/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable Long id) { rooms.deletePermanently(id); }

    // ----- bookings -----
    @GetMapping("/bookings")
    public List<BookingResponse> bookings() { return bookings.all(); }

    @PutMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        return bookings.cancel(id, auth.getName(), true);
    }

    // ----- guests -----
    @GetMapping("/users")
    public List<UserResponse> users() { return bookings.allUsers(); }
}
