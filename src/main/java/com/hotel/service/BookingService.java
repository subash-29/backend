package com.hotel.service;

import com.hotel.dto.Dtos.*;
import com.hotel.entity.*;
import com.hotel.exception.ApiException;
import com.hotel.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final PaymentRepository payments;

    public BookingService(BookingRepository bookings, RoomRepository rooms,
                          UserRepository users, PaymentRepository payments) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.users = users;
        this.payments = payments;
    }

    static void validateDates(LocalDate in, LocalDate out) {
        if (in.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Check-in date cannot be in the past");
        }
        if (!out.isAfter(in)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Check-out must be after check-in");
        }
    }

    /**
     * Double-booking protection:
     *  1. Everything runs in ONE transaction.
     *  2. The room row is locked with SELECT ... FOR UPDATE, so concurrent requests for the same room queue up.
     *  3. Only after the lock is held do we run the overlap check, so the second request sees the first one's commit.
     */
    @Transactional
    public BookingResponse create(String email, BookingRequest req) {
        validateDates(req.checkIn(), req.checkOut());

        User user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Room room = rooms.findByIdForUpdate(req.roomId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!room.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Room is not available for booking");
        }
        if (req.guests() > room.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Room capacity is " + room.getCapacity() + " guests");
        }
        if (bookings.existsOverlap(room.getId(), req.checkIn(), req.checkOut())) {
            throw new ApiException(HttpStatus.CONFLICT, "Room is already booked for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(req.checkIn(), req.checkOut());
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = bookings.save(Booking.builder()
                .user(user).room(room)
                .checkIn(req.checkIn()).checkOut(req.checkOut())
                .guests(req.guests()).totalPrice(total)
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build());

        // Simulated payment - plug a real gateway (Stripe/Razorpay) in here.
        Payment payment = payments.save(Payment.builder()
                .booking(booking).amount(total)
                .status(PaymentStatus.PAID)
                .reference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paidAt(LocalDateTime.now())
                .build());

        return toDto(booking, payment);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(String email) {
        return bookings.findByUserEmail(email).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> all() {
        return bookings.findAllWithDetails().stream().map(this::toDto).toList();
    }

    @Transactional
    public BookingResponse cancel(Long id, String email, boolean isAdmin) {
        Booking b = bookings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!isAdmin && !b.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own bookings");
        }
        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking is already cancelled");
        }
        if (!isAdmin && !b.getCheckIn().isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bookings can only be cancelled before the check-in date");
        }
        b.setStatus(BookingStatus.CANCELLED);
        Payment p = payments.findByBookingId(b.getId()).orElse(null);
        if (p != null && p.getStatus() == PaymentStatus.PAID) {
            p.setStatus(PaymentStatus.REFUNDED);
        }
        return toDto(b, p);
    }

    /** Every room (active or not) with the confirmed bookings that touch [from, to). */
    @Transactional(readOnly = true)
    public List<RoomOccupancy> occupancy(LocalDate from, LocalDate to) {
        if (!to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'to' must be after 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > 62) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Range is limited to 62 days");
        }
        Map<Long, List<BookingBrief>> byRoom = new HashMap<>();
        for (Booking b : bookings.findConfirmedOverlapping(from, to)) {
            byRoom.computeIfAbsent(b.getRoom().getId(), k -> new ArrayList<>()).add(new BookingBrief(
                    b.getId(), b.getUser().getName(), b.getUser().getEmail(),
                    b.getCheckIn(), b.getCheckOut(), b.getGuests(), b.getTotalPrice()));
        }
        return rooms.findAll(Sort.by("roomNumber")).stream()
                .map(r -> new RoomOccupancy(RoomService.toDto(r), byRoom.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> allUsers() {
        return users.findAll(Sort.by("id")).stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(),
                        bookings.countByUserId(u.getId())))
                .toList();
    }

    public StatsResponse stats() {
        return new StatsResponse(rooms.count(), users.count(), bookings.count(),
                bookings.countByStatus(BookingStatus.CONFIRMED),
                bookings.countByStatus(BookingStatus.CANCELLED),
                payments.sumByStatus(PaymentStatus.PAID));
    }

    private BookingResponse toDto(Booking b) {
        return toDto(b, payments.findByBookingId(b.getId()).orElse(null));
    }

    private BookingResponse toDto(Booking b, Payment p) {
        return new BookingResponse(b.getId(), b.getRoom().getId(), b.getRoom().getRoomNumber(), b.getRoom().getType(),
                b.getUser().getName(), b.getUser().getEmail(),
                b.getCheckIn(), b.getCheckOut(), b.getGuests(), b.getTotalPrice(), b.getStatus(),
                p == null ? null : p.getStatus(), p == null ? null : p.getReference(), b.getCreatedAt());
    }
}
