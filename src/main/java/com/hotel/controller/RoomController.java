package com.hotel.controller;

import com.hotel.dto.Dtos.RoomResponse;
import com.hotel.entity.RoomType;
import com.hotel.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService rooms;

    public RoomController(RoomService rooms) { this.rooms = rooms; }

    @GetMapping
    public List<RoomResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return rooms.search(checkIn, checkOut, type, guests, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) { return rooms.get(id); }
}
