package com.finpay.notification.repository;

import com.finpay.notification.entity.Notification;
import com.finpay.notification.entity.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndIsReadFalse(String userId);

    long countByUserIdAndIsReadFalse(String userId);

    List<Notification> findByUserIdAndType(String userId, NotificationType type);
}
