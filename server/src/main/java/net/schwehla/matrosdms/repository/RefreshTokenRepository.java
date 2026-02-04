package net.schwehla.matrosdms.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.schwehla.matrosdms.entity.management.DBRefreshToken;
import net.schwehla.matrosdms.entity.management.DBUser;

@Repository
public interface RefreshTokenRepository extends JpaRepository<DBRefreshToken, Long> {
    
    Optional<DBRefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(DBUser user);

    @Modifying
    @Query("DELETE FROM DBRefreshToken t WHERE t.expiryDate < :now")
    int deleteExpiredTokens(Instant now);
}