package com.dmall.inventoryservice.applications;

import com.dmall.inventoryservice.domain.Inventory;
import com.dmall.inventoryservice.domain.InventoryLock;
import com.dmall.inventoryservice.domain.event.InventoryEvent;
import com.dmall.inventoryservice.infrastructure.mq.EventStreams;
import com.dmall.inventoryservice.infrastructure.repositories.InventoryLockRepository;
import com.dmall.inventoryservice.infrastructure.repositories.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryLockRepository inventoryLockRepository;

    @Autowired
    private EventStreams eventStreams;

    public void save(Inventory inventory) {
        inventoryRepository.save(inventory);
    }

    public Long lock(InventoryLock inventoryLock) {
        Long lockId = inventoryLockRepository.save(inventoryLock);
        return lockId;
    }

    public Long lock(InventoryLock inventoryLock, String orderId) {
        Long lockId = inventoryLockRepository.save(inventoryLock);
        eventStreams.outputInventory().send(MessageBuilder.withPayload(InventoryEvent.lockedEvent(inventoryLock,
                orderId, lockId))
                .build());
        return lockId;
    }

    @Transactional
    public void unlock(long id) {
        InventoryLock inventoryLock = inventoryLockRepository.findById(id);
        inventoryLockRepository.deleteById(id);
        Inventory inventory = inventoryRepository.findByProductId(inventoryLock.getProductId());
        inventory.deductProduct(inventoryLock.getQuantity());
        inventoryRepository.updateInventory(inventory);

        eventStreams.outputInventory().send(MessageBuilder.withPayload(InventoryEvent.unlockedEvent(inventoryLock))
                .build());
    }
}
