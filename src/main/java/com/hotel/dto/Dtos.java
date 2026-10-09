package com.hotel.dto;

import com.hotel.entity.BookingStatus;
import com.hotel.entity.PaymentStatus;
import com.hotel.entity.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    public record RegisterRequest(@NotBlank String name,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long id, String name, String email, String role) {}

    public record RoomRequest(@NotBlank String roomNumber,
                              @NotNull RoomType type,
                              @Min(1) int capacity,
                              @NotNull @DecimalMin("0.0") BigDecimal pricePerNight,
                              String description,
                              String imageUrl,
                              Boolean active) {}

    public record RoomResponse(Long id, String roomNumber, RoomType type, int capacity,
                               BigDecimal pricePerNight, String description, String imageUrl, boolean active) {}

    public record BookingRequest(@NotNull Long roomId,
                                 @NotNull LocalDate checkIn,
                                 @NotNull LocalDate checkOut,
                                 @Min(1) int guests) {}

    public record BookingResponse(Long id, Long roomId, String roomNumber, RoomType roomType,
                                  String guestName, String guestEmail,
                                  LocalDate checkIn, LocalDate checkOut, int guests,
                                  BigDecimal totalPrice, BookingStatus status,
                                  PaymentStatus paymentStatus, String paymentReference,
                                  LocalDateTime createdAt) {}

    public record StatsResponse(long totalRooms, long totalUsers, long totalBookings,
                                long confirmedBookings, long cancelledBookings, BigDecimal revenue) {}

    public record BookingBrief(Long id, String guestName, String guestEmail,
                               LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal totalPrice) {}

    public record RoomOccupancy(RoomResponse room, List<BookingBrief> bookings) {}

    public record UserResponse(Long id, String name, String email, String role, long bookings) {}
}
