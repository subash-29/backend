package com.hotel.service;

import com.hotel.dto.Dtos.*;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.exception.ApiException;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RoomService {
    private final RoomRepository rooms;
    private final BookingRepository bookingRepo;

    public RoomService(RoomRepository rooms, BookingRepository bookingRepo) {
        this.rooms = rooms;
        this.bookingRepo = bookingRepo;
    }

    /** Search by optional dates / type / capacity / price. Dates, when given, must both be present. */
    public List<RoomResponse> search(LocalDate checkIn, LocalDate checkOut, RoomType type,
                                     Integer guests, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Room> base;
        if (checkIn != null && checkOut != null) {
            BookingService.validateDates(checkIn, checkOut);
            base = rooms.findAvailable(checkIn, checkOut);
        } else if (checkIn == null && checkOut == null) {
            base = rooms.findByActiveTrueOrderByPricePerNightAsc();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide both check-in and check-out dates");
        }
        return base.stream()
                .filter(r -> type == null || r.getType() == type)
                .filter(r -> guests == null || r.getCapacity() >= guests)
                .filter(r -> minPrice == null || r.getPricePerNight().compareTo(minPrice) >= 0)
                .filter(r -> maxPrice == null || r.getPricePerNight().compareTo(maxPrice) <= 0)
                .map(RoomService::toDto)
                .toList();
    }

    public RoomResponse get(Long id) {
        return toDto(rooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found")));
    }

    // ----- admin -----
    public List<RoomResponse> all() {
        return rooms.findAll().stream().map(RoomService::toDto).toList();
    }

    public RoomResponse create(RoomRequest req) {
        if (rooms.existsByRoomNumber(req.roomNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Room number already exists");
        }
        Room room = new Room();
        apply(room, req);
        return toDto(rooms.save(room));
    }

    public RoomResponse update(Long id, RoomRequest req) {
        Room room = rooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        if (!room.getRoomNumber().equals(req.roomNumber()) && rooms.existsByRoomNumber(req.roomNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Room number already exists");
        }
        apply(room, req);
        return toDto(rooms.save(room));
    }

    /** Soft delete - keeps booking history intact. */
    public void deactivate(Long id) {
        Room room = rooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        room.setActive(false);
        rooms.save(room);
    }

    public RoomResponse setActive(Long id, boolean active) {
        Room room = rooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        room.setActive(active);
        return toDto(rooms.save(room));
    }

    /** Hard delete - only allowed when the room has no booking history. */
    public void deletePermanently(Long id) {
        Room room = rooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        if (bookingRepo.existsByRoomId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Room has booking history - deactivate it instead of deleting");
        }
        rooms.delete(room);
    }

    private void apply(Room r, RoomRequest q) {
        r.setRoomNumber(q.roomNumber().trim());
        r.setType(q.type());
        r.setCapacity(q.capacity());
        r.setPricePerNight(q.pricePerNight());
        r.setDescription(q.description());
        r.setImageUrl(q.imageUrl());
        r.setActive(q.active() == null || q.active());
    }

    static RoomResponse toDto(Room r) {
        return new RoomResponse(r.getId(), r.getRoomNumber(), r.getType(), r.getCapacity(),
                r.getPricePerNight(), r.getDescription(), r.getImageUrl(), r.isActive());
    }
}
