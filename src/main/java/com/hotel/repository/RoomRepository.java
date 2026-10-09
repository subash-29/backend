package com.hotel.repository;

import com.hotel.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByActiveTrueOrderByPricePerNightAsc();

    /** Row-level lock (SELECT ... FOR UPDATE) - serialises concurrent bookings of the same room. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    /** Active rooms that have NO confirmed booking overlapping [checkIn, checkOut). */
    @Query("""
            select r from Room r
            where r.active = true
              and not exists (
                select b.id from Booking b
                where b.room = r
                  and b.status = com.hotel.entity.BookingStatus.CONFIRMED
                  and b.checkIn < :checkOut
                  and b.checkOut > :checkIn)
            order by r.pricePerNight asc
            """)
    List<Room> findAvailable(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
}
