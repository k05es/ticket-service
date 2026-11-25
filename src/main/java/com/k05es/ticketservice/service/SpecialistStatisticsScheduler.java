package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Сбрасывает суточные счётчики специалистов в начале новых суток. */
@Component
public class SpecialistStatisticsScheduler {

    private final UserRepository userRepository;

    public SpecialistStatisticsScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.statistics.time-zone:UTC}")
    @Transactional
    public void resetDailyCounters() {
        List<User> specialists = userRepository.findByRoleForUpdate(UserRole.SPECIALIST);
        for (User user : specialists) {
            if (user instanceof SpecialistUser specialist) {
                specialist.resetCompletedTodayCount();
            }
        }
        userRepository.saveAll(specialists);
    }
}