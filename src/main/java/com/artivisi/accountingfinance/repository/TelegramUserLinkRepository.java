package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.TelegramUserLink;
import com.artivisi.accountingfinance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramUserLinkRepository extends JpaRepository<TelegramUserLink, UUID> {

    Optional<TelegramUserLink> findByTelegramUserId(Long telegramUserId);

    Optional<TelegramUserLink> findByTelegramUserIdAndIsActiveTrue(Long telegramUserId);

    Optional<TelegramUserLink> findByUser(User user);

    Optional<TelegramUserLink> findByUserAndIsActiveTrue(User user);

    Optional<TelegramUserLink> findByVerificationCode(String verificationCode);

    boolean existsByTelegramUserId(Long telegramUserId);
}
