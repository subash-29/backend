package com.hotel.repository;

import com.hotel.entity.Booking;
import com.hotel.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b join fetch b.room join fetch b.user where b.user.email = :email order by b.createdAt desc")
    List<Booking> findByUserEmail(@Param("email") String email);

    @Query("select b from Booking b join fetch b.room join fetch b.user order by b.createdAt desc")
    List<Booking> findAllWithDetails();

    long countByStatus(BookingStatus status);

    long countByUserId(Long userId);

    boolean existsByRoomId(Long roomId);

    /** Confirmed bookings overlapping [rangeStart, rangeEnd) - used by the admin occupancy board. */
    @Query("""
            select b from Booking b join fetch b.room join fetch b.user
            where b.status = com.hotel.entity.BookingStatus.CONFIRMED
              and b.checkIn < :rangeEnd
              and b.checkOut > :rangeStart
            """)
    List<Booking> findConfirmedOverlapping(@Param("rangeStart") LocalDate rangeStart,
                                           @Param("rangeEnd") LocalDate rangeEnd);

    /** Two ranges overlap when existing.checkIn < new.checkOut AND existing.checkOut > new.checkIn. */
    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.status = com.hotel.entity.BookingStatus.CONFIRMED
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut);
}
